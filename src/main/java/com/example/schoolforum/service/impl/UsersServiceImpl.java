package com.example.schoolforum.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.example.schoolforum.constant.RedisCacheKey;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.util.UpdateEntity;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.example.schoolforum.pojo.Users;
import com.example.schoolforum.mapper.UsersMapper;
import com.example.schoolforum.service.UsersService;
import com.example.schoolforum.service.SearchService;
import com.example.schoolforum.enums.CodeType;
import com.example.schoolforum.enums.ActiveStatus;
import com.example.schoolforum.enums.Gender;
import com.example.schoolforum.enums.UserRole;
import com.example.schoolforum.exception.BusinessException;
import com.example.schoolforum.config.FileUploadProperties;
import com.example.schoolforum.pojo.dto.LoginResponse;
import com.example.schoolforum.pojo.dto.UserSearchDocument;
import com.example.schoolforum.util.FileUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 用户表 服务层实现。
 *
 * @author sugu
 * @since 2026-02-17
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UsersServiceImpl extends ServiceImpl<UsersMapper, Users> implements UsersService {

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    private JavaMailSender mailSender;
    private final StringRedisTemplate redisTemplate;
    private final FileUploadProperties fileUploadProperties;
    private final PasswordEncoder passwordEncoder;
    private final SearchService searchService;
    private final ObjectMapper objectMapper;

    @Value("${spring.mail.username}")
    private String mailUsername;

    private static final long CAPTCHA_EXPIRE_TIME = 5;
    private static final String CAPTCHA_PREFIX = "captcha:";
    private static final Set<String> ALLOWED_IMAGE_EXTENSIONS = Set.of(".jpg", ".jpeg", ".png", ".gif", ".webp", ".bmp");

    @Override
    public void sendCaptcha(String email, CodeType codeType) {
        String rateLimitKey = RedisCacheKey.CAPTCHA_RATE_LIMIT + email;
        String lastSentTime = (String) redisTemplate.opsForValue().get(rateLimitKey);
        if (lastSentTime != null) {
            throw new BusinessException("发送过于频繁，请60秒后再试");
        }

        String captcha = generateCaptcha();
        String redisKey = getRedisKey(email, codeType);
        redisTemplate.opsForValue().set(redisKey, captcha, CAPTCHA_EXPIRE_TIME, TimeUnit.MINUTES);
        redisTemplate.opsForValue().set(rateLimitKey, "1", 60, TimeUnit.SECONDS);
        sendEmail(email, codeType, captcha);
        log.info("验证码已发送: email={}, type={}", email, codeType.getDesc());
    }

    @Override
    public boolean verifyCaptcha(String email, CodeType codeType, String captcha) {
        String redisKey = getRedisKey(email, codeType);
        String errorCountKey = RedisCacheKey.CAPTCHA_ERROR_COUNT + email;
        String errorCountStr = (String) redisTemplate.opsForValue().get(errorCountKey);
        int errorCount = errorCountStr != null ? Integer.parseInt(errorCountStr) : 0;
        if (errorCount >= 5) {
            redisTemplate.delete(redisKey);
            redisTemplate.delete(errorCountKey);
            return false;
        }

        String storedCaptcha = redisTemplate.opsForValue().get(redisKey);
        if (storedCaptcha != null && storedCaptcha.equals(captcha)) {
            redisTemplate.delete(redisKey);
            redisTemplate.delete(errorCountKey);
            return true;
        }
        redisTemplate.opsForValue().set(errorCountKey, String.valueOf(errorCount + 1), 5, TimeUnit.MINUTES);
        return false;
    }

    @Override
    @Transactional
    public Users register(String username, String password, String email, Integer age, Gender gender, String captcha) {
        validatePassword(password);

        if (!verifyCaptcha(email, CodeType.REGISTER, captcha)) {
            throw new BusinessException("验证码错误或已过期");
        }
        
        Users existUser = getMapper().selectOneByQuery(
            QueryWrapper.create().where(Users::getUsername).eq(username)
        );
        if (existUser != null) {
            throw new BusinessException("用户名已存在");
        }

        Users existEmail = getMapper().selectOneByQuery(
            QueryWrapper.create().where(Users::getEmail).eq(email)
        );
        if (existEmail != null) {
            throw new BusinessException("邮箱已被注册");
        }
        
        Users users = new Users();
        users.setUsername(username);
        users.setPassword(passwordEncoder.encode(password));
        users.setEmail(email);
        users.setAge(age);
        users.setGender(gender != null ? gender : Gender.SECRET);
        users.setRole(UserRole.USER);
        users.setIsActive(ActiveStatus.ACTIVE);
        users.setShowFollowing(false);
        users.setShowFollowers(false);
        users.setLevel(1);
        users.setExp(0);
        users.setPoints(0);
        users.setContinuousSignDays(0);
        users.setSignCards(0);
        users.setCreatedAt(LocalDateTime.now());
        users.setUpdatedAt(LocalDateTime.now());
        users.setLastLoginAt(LocalDateTime.now());
        
        try {
            this.save(users);
        } catch (DuplicateKeyException e) {
            throw new BusinessException("用户名或邮箱已存在");
        }
        searchService.indexUser(UserSearchDocument.fromEntity(users));
        
        log.info("用户注册成功: username={}, email={}", username, email);
        users.setPassword(null);
        return users;
    }

    @Override
    @Transactional
    public LoginResponse login(String username, String password) {
        Users existUser = getMapper().selectOneByQuery(
            QueryWrapper.create().where(Users::getUsername).eq(username)
        );
        if (existUser == null) {
            throw new BusinessException("用户不存在");
        }
        
        if (!passwordEncoder.matches(password, existUser.getPassword())) {
            throw new BusinessException("密码错误");
        }
        
        if (existUser.getIsActive() == ActiveStatus.INACTIVE) {
            throw new BusinessException("账户未激活，请联系管理员");
        }
        
        Users update = UpdateEntity.of(Users.class, existUser.getId());
        update.setLastLoginAt(LocalDateTime.now());
        getMapper().update(update);
        
        StpUtil.login(existUser.getId());
        if (existUser.getRole() != null) {
            StpUtil.getSession().set("roles",
                    Collections.singletonList(existUser.getRole().name().toLowerCase()));
        }
        String token = StpUtil.getTokenValue();
        
        existUser.setPassword(null);
        cacheUserInfo(existUser);
        
        log.info("用户登录成功: username={}", username);
        return new LoginResponse(existUser, token);
    }

    @Override
    public Users getByUsername(String username) {
        return getMapper().selectOneByQuery(
            QueryWrapper.create().where(Users::getUsername).eq(username)
        );
    }

    @Override
    public Users getByEmail(String email) {
        return getMapper().selectOneByQuery(
            QueryWrapper.create().where(Users::getEmail).eq(email)
        );
    }

    @Override
    public Users getByGithubId(String githubId) {
        return getMapper().selectOneByQuery(
            QueryWrapper.create().where(Users::getGithubId).eq(githubId)
        );
    }

    @Override
    @Transactional
    public Users updateUser(Long targetId, String username, String password, String email, Integer age, Gender gender, String bio, UserRole role, boolean isAdmin, boolean isSuperAdmin) {
        Users users = this.getById(targetId);
        if (users == null) {
            throw new BusinessException("更新失败，用户不存在");
        }
        
        UserRole oldRole = users.getRole();
        
        if (username != null && !username.isEmpty()) users.setUsername(username);
        if (password != null && !password.isEmpty()) users.setPassword(passwordEncoder.encode(password));
        if (email != null && !email.isEmpty()) users.setEmail(email);
        if (age != null) users.setAge(age);
        if (gender != null) users.setGender(gender);
        if (bio != null) users.setBio(bio);
        if (role != null) {
            if (role == UserRole.SUPER_ADMIN && !isSuperAdmin) {
                throw new BusinessException("仅超级管理员可设置超级管理员角色");
            }
            if (isSuperAdmin || isAdmin) {
                users.setRole(role);
            }
        }
        users.setUpdatedAt(LocalDateTime.now());
        
        this.updateById(users);
        searchService.indexUser(UserSearchDocument.fromEntity(users));
        evictUserCache(targetId);
        
        if (role != null && !role.equals(oldRole) && StpUtil.isLogin(targetId)) {
            StpUtil.getSessionByLoginId(targetId).set("roles",
                    Collections.singletonList(role.name().toLowerCase()));
            log.debug("用户角色已更新: userId={}, oldRole={}, newRole={}", targetId, oldRole, role);
        }
        
        log.info("用户信息更新成功: userId={}", targetId);
        users.setPassword(null);
        return users;
    }

    @Override
    public Page<Users> list(int pageNumber, int pageSize) {
        return listPage(pageNumber, pageSize);
    }

    @Override
    public Page<Users> listPage(int pageNumber, int pageSize) {
        Page<Users> result = getMapper().paginate(pageNumber, pageSize, QueryWrapper.create());
        result.getRecords().forEach(u -> u.setPassword(null));
        return result;
    }

    private void validatePassword(String password) {
        if (password == null || password.length() < 8) {
            throw new BusinessException("密码长度不能少于8位");
        }
        if (!password.matches(".*[a-zA-Z].*") || !password.matches(".*\\d.*")) {
            throw new BusinessException("密码必须包含字母和数字");
        }
    }

    @Override
    @Transactional
    public void changePassword(Long userId, String oldPassword, String newPassword, String captcha) {
        validatePassword(newPassword);

        Users users = this.getById(userId);
        if (users == null) {
            throw new BusinessException("用户不存在");
        }
        
        if (!passwordEncoder.matches(oldPassword, users.getPassword())) {
            throw new BusinessException("旧密码错误");
        }
        
        if (passwordEncoder.matches(newPassword, users.getPassword())) {
            throw new BusinessException("新密码不能与旧密码相同");
        }
        
        if (!verifyCaptcha(users.getEmail(), CodeType.CHANGE_PASSWORD, captcha)) {
            throw new BusinessException("验证码错误或已过期");
        }
        
        Users update = UpdateEntity.of(Users.class, userId);
        update.setPassword(passwordEncoder.encode(newPassword));
        update.setUpdatedAt(LocalDateTime.now());
        getMapper().update(update);
        
        log.info("密码修改成功: userId={}", userId);
    }

    @Override
    @Transactional
    public void resetPassword(String email, String newPassword, String captcha) {
        validatePassword(newPassword);

        if (!verifyCaptcha(email, CodeType.RESET_PASSWORD, captcha)) {
            throw new BusinessException("验证码错误或已过期");
        }
        
        Users users = getByEmail(email);
        if (users == null) {
            throw new BusinessException("邮箱未注册");
        }
        
        Users update = UpdateEntity.of(Users.class, users.getId());
        update.setPassword(passwordEncoder.encode(newPassword));
        update.setUpdatedAt(LocalDateTime.now());
        getMapper().update(update);
        
        log.info("密码重置成功: email={}", email);
    }

    @Override
    public String uploadAvatar(Long userId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("上传文件不能为空");
        }

        String contentType = file.getContentType();
        String allowedTypes = fileUploadProperties.getAllowedTypes();
        if (contentType == null || !Arrays.asList(allowedTypes.split(",")).contains(contentType)) {
            throw new BusinessException("不支持的文件类型，仅支持: " + allowedTypes);
        }

        long maxSize = FileUtil.parseSize(fileUploadProperties.getMaxSize());
        if (file.getSize() > maxSize) {
            throw new BusinessException("文件大小超过限制，最大允许: " + fileUploadProperties.getMaxSize());
        }

        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        if (!ALLOWED_IMAGE_EXTENSIONS.contains(extension.toLowerCase())) {
            throw new BusinessException("不支持的图片格式，仅允许: jpg, jpeg, png, gif, webp, bmp");
        }
        String newFilename = UUID.randomUUID()+ extension;

        try {
            Path uploadPath = Paths.get(fileUploadProperties.getAvatarPath());
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            Path filePath = uploadPath.resolve(newFilename);
            Files.copy(file.getInputStream(), filePath);

            Users user = this.getById(userId);
            if (user == null) {
                Files.deleteIfExists(filePath);
                throw new BusinessException("用户不存在");
            }

            String oldAvatar = user.getAvatarUrl();
            String avatarUrl = "/avatars/" + newFilename;
            
            Users update = UpdateEntity.of(Users.class, userId);
            update.setAvatarUrl(avatarUrl);
            update.setUpdatedAt(LocalDateTime.now());
            getMapper().update(update);
            evictUserCache(userId);

            // Delete old avatar after DB update succeeds
            if (oldAvatar != null && oldAvatar.contains("/avatars/")) {
                String oldFilename = oldAvatar.substring(oldAvatar.lastIndexOf("/") + 1);
                Path oldFilePath = uploadPath.resolve(oldFilename);
                Files.deleteIfExists(oldFilePath);
            }

            log.info("头像上传成功: userId={}, avatarUrl={}", userId, avatarUrl);
            return avatarUrl;

        } catch (IOException e) {
            log.error("头像上传失败: userId={}, error={}", userId, e.getMessage(), e);
            throw new BusinessException("头像上传失败: " + e.getMessage());
        }
    }

    private String generateCaptcha() {
        SecureRandom random = new SecureRandom();
        StringBuilder captcha = new StringBuilder();
        for (int i = 0; i < 6; i++) {
            captcha.append(random.nextInt(10));
        }
        return captcha.toString();
    }

    private String getRedisKey(String email, CodeType codeType) {
        return CAPTCHA_PREFIX + codeType.getCode() + ":" + email;
    }

    private void sendEmail(String to, CodeType codeType, String captcha) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(mailUsername, "校园论坛");
            helper.setTo(to);
            helper.setSubject("校园论坛 - " + codeType.getDesc() + "验证码");
            helper.setText(buildEmailTextContent(codeType, captcha), buildEmailHtmlContent(codeType, captcha));
            mailSender.send(message);
        } catch (MessagingException | java.io.UnsupportedEncodingException e) {
            throw new BusinessException("邮件发送失败");
        }
    }

    private String buildEmailTextContent(CodeType codeType, String captcha) {
        return String.format("""
            亲爱的用户：

            　　您正在%s。

            　　你的code:

            %s

            　　%d分钟内有效，请尽快使用。如非本人操作，请忽略此邮件。

            校园论坛团队

            此邮件由系统自动发送，请勿直接回复。
            """, codeType.getDesc(), captcha, CAPTCHA_EXPIRE_TIME);
    }

    private String buildEmailHtmlContent(CodeType codeType, String captcha) {
        return String.format("""
            <!DOCTYPE html>
            <html lang="zh-CN">
            <head>
                <meta charset="UTF-8">
                <style>
                    body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Microsoft YaHei', sans-serif; background-color: #f5f5f5; margin: 0; padding: 20px; }
                    .container { max-width: 600px; margin: 0 auto; background-color: #ffffff; border-radius: 8px; overflow: hidden; box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1); }
                    .header { height: 8px; }
                    .content { padding: 40px 30px; }
                    .salutation { color: #1d2129; font-size: 16px; margin: 0 0 24px; }
                    .paragraph { color: #1d2129; font-size: 14px; line-height: 1.8; margin: 0 0 16px; text-indent: 2em; }
                    .code-label { color: #1d2129; font-size: 18px; margin: 0 0 16px; text-indent: 2em; }
                    .code-wrapper { text-align: center; }
                    .code { font-size: 36px; font-weight: bold; letter-spacing: 8px; color: #1d2129; background-color: #f2f3f5; padding: 20px 30px; border-radius: 8px; display: inline-block; margin: 4px 0 24px; font-family: monospace; }
                    .signature { color: #1d2129; font-size: 14px; line-height: 1.8; margin: 24px 0 0; text-align: right; }
                    .footer { background-color: #f7f8fa; padding: 20px; text-align: center; color: #86909c; font-size: 12px; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header"></div>
                    <div class="content">
                        <p class="salutation">亲爱的用户：</p>
                        <p class="paragraph">您正在%s。</p>
                        <p class="code-label">你的code:</p>
                        <div class="code-wrapper"><div class="code">%s</div></div>
                        <p class="paragraph"><strong>%d分钟</strong>内有效，请尽快使用。如非本人操作，请忽略此邮件。</p>
                        <p class="signature">校园论坛团队</p>
                    </div>
                    <div class="footer">
                        <p>此邮件由系统自动发送，请勿直接回复。</p>
                    </div>
                </div>
            </body>
            </html>
            """, codeType.getDesc(), captcha, CAPTCHA_EXPIRE_TIME);
    }

    @Override
    @Transactional
    public Users createGithubUser(String githubId, String username, String email, String avatarUrl) {
        Users user = new Users();
        user.setUsername(generateUniqueUsername(username));
        user.setPassword(passwordEncoder.encode(generateRandomPassword()));
        user.setGithubId(githubId);
        user.setAvatarUrl(avatarUrl);
        user.setEmail(email);
        user.setRole(UserRole.USER);
        user.setGender(Gender.SECRET);
        user.setIsActive(ActiveStatus.ACTIVE);
        user.setShowFollowing(false);
        user.setShowFollowers(false);
        user.setLevel(1);
        user.setExp(0);
        user.setPoints(0);
        user.setContinuousSignDays(0);
        user.setSignCards(0);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        user.setLastLoginAt(LocalDateTime.now());
        this.save(user);
        
        log.info("GitHub用户创建成功: githubId={}, username={}", githubId, user.getUsername());
        return user;
    }

    @Override
    @Transactional
    public Users updateGithubUser(Users user, String avatarUrl) {
        user.setLastLoginAt(LocalDateTime.now());
        user.setAvatarUrl(avatarUrl);
        this.updateById(user);
        return user;
    }

    private String generateRandomPassword() {
        SecureRandom random = new SecureRandom();
        StringBuilder password = new StringBuilder();
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        for (int i = 0; i < 16; i++) {
            password.append(chars.charAt(random.nextInt(chars.length())));
        }
        return password.toString();
    }

    private String generateUniqueUsername(String baseUsername) {
        if (baseUsername == null || baseUsername.isEmpty()) {
            baseUsername = "github_user";
        }
        String username = baseUsername;
        int suffix = 1;
        int maxRetries = 10;
        while (maxRetries > 0) {
            try {
                Users existing = getMapper().selectOneByQuery(
                    QueryWrapper.create().where(Users::getUsername).eq(username)
                );
                if (existing == null) {
                    return username;
                }
            } catch (DuplicateKeyException e) {
                // 并发冲突，重试
            }
            username = baseUsername + "_" + suffix;
            suffix++;
            maxRetries--;
        }
        return username;
    }

    @Override
    public Users getCachedUserById(Long userId) {
        String cacheKey = RedisCacheKey.userInfoKey(userId);
        try {
            String cached = redisTemplate.opsForValue().get(cacheKey);
            if (cached != null) {
                return objectMapper.readValue(cached, Users.class);
            }
        } catch (Exception e) {
            log.warn("读取用户缓存失败: userId={}", userId, e);
        }

        Users user = this.getById(userId);
        if (user != null) {
            user.setPassword(null);
            try {
                redisTemplate.opsForValue().set(cacheKey, objectMapper.writeValueAsString(user), 
                        RedisCacheKey.USER_INFO_TTL, TimeUnit.SECONDS);
            } catch (Exception e) {
                log.warn("写入用户缓存失败: userId={}", userId, e);
            }
        }
        
        return user;
    }

    @Override
    public void evictUserCache(Long userId) {
        String cacheKey = RedisCacheKey.userInfoKey(userId);
        redisTemplate.delete(cacheKey);
        log.debug("清除用户缓存: userId={}", userId);
    }

    @Override
    public void cacheUserInfo(Users user) {
        if (user == null || user.getId() == null) {
            return;
        }
        String cacheKey = RedisCacheKey.userInfoKey(user.getId());
        try {
            redisTemplate.opsForValue().set(cacheKey, objectMapper.writeValueAsString(user), 
                    RedisCacheKey.USER_INFO_TTL, TimeUnit.SECONDS);
            log.debug("用户信息已缓存: userId={}", user.getId());
        } catch (Exception e) {
            log.warn("缓存用户信息失败: userId={}", user.getId(), e);
        }
    }

    @Override
    @Transactional
    public void bindGithub(Long userId, String githubId, String avatarUrl) {
        Users user = this.getById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        
        Users update = UpdateEntity.of(Users.class, userId);
        update.setGithubId(githubId);
        if (avatarUrl != null && !avatarUrl.isEmpty()) {
            update.setAvatarUrl(avatarUrl);
        }
        update.setUpdatedAt(LocalDateTime.now());
        getMapper().update(update);
        
        evictUserCache(userId);
        log.info("绑定GitHub成功: userId={}, githubId={}", userId, githubId);
    }

    @Override
    @Transactional
    public void unbindGithub(Long userId) {
        Users user = this.getById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        
        Users update = UpdateEntity.of(Users.class, userId);
        update.setGithubId(null);
        update.setUpdatedAt(LocalDateTime.now());
        getMapper().update(update);
        
        evictUserCache(userId);
        log.info("解绑GitHub成功: userId={}", userId);
    }

    @Override
    @Transactional
    public void updatePrivacy(Long userId, Boolean showFollowing, Boolean showFollowers) {
        Users user = this.getById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        
        Users update = UpdateEntity.of(Users.class, userId);
        if (showFollowing != null) {
            update.setShowFollowing(showFollowing);
        }
        if (showFollowers != null) {
            update.setShowFollowers(showFollowers);
        }
        update.setUpdatedAt(LocalDateTime.now());
        getMapper().update(update);
        
        evictUserCache(userId);
        log.info("隐私设置更新成功: userId={}, showFollowing={}, showFollowers={}", userId, showFollowing, showFollowers);
    }
}
