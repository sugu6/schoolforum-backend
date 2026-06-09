package com.example.schoolforum.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.example.schoolforum.config.GitHubOAuthProperties;
import com.example.schoolforum.exception.BusinessException;
import com.example.schoolforum.pojo.Users;
import com.example.schoolforum.pojo.dto.OAuthCallbackResult;
import com.example.schoolforum.service.UsersService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.zhyd.oauth.config.AuthConfig;
import me.zhyd.oauth.model.AuthCallback;
import me.zhyd.oauth.model.AuthResponse;
import me.zhyd.oauth.model.AuthUser;
import me.zhyd.oauth.request.AuthGithubRequest;
import me.zhyd.oauth.request.AuthRequest;
import me.zhyd.oauth.utils.AuthStateUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;
import com.example.schoolforum.util.SslUtils;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@RestController
@RequestMapping("/oauth")
@RequiredArgsConstructor
@Tag(name = "OAuth第三方登录", description = "第三方登录相关接口")
public class OAuthController {

    private final GitHubOAuthProperties gitHubOAuthProperties;
    private final UsersService usersService;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    private static final String OAUTH_TEMP_KEY_PREFIX = "oauth:temp:";
    private static final String BIND_STATE_PREFIX = "bind:";
    private static final String OAUTH_STATE_PREFIX = "oauth:state:";
    private static final long OAUTH_TEMP_EXPIRE_TIME = 10;

    @GetMapping("/render/github")
    @Operation(summary = "GitHub授权页面", description = "跳转到GitHub授权页面（用于登录）")
    public void renderGithubAuth(HttpServletResponse response) throws IOException {
        AuthRequest authRequest = getGithubAuthRequest();
        String state = UUID.randomUUID().toString().replace("-", "");
        redisTemplate.opsForValue().set(OAUTH_STATE_PREFIX + state, "1", 10, TimeUnit.MINUTES);
        response.sendRedirect(authRequest.authorize(state));
    }

    @GetMapping("/bind/github")
    @Operation(summary = "绑定GitHub授权页面", description = "跳转到GitHub授权页面（用于绑定账号），需要登录")
    public void bindGithubAuth(HttpServletResponse response) throws IOException {
        Long userId = StpUtil.getLoginIdAsLong();
        Users user = usersService.getById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        if (user.getGithubId() != null && !user.getGithubId().isEmpty()) {
            throw new BusinessException("您已绑定GitHub账号，请先解绑后再绑定新账号");
        }
        
        AuthRequest authRequest = getGithubAuthRequest();
        String rawState = UUID.randomUUID().toString().replace("-", "");
        String state = BIND_STATE_PREFIX + rawState;
        redisTemplate.opsForValue().set(OAUTH_STATE_PREFIX + state, String.valueOf(userId), 10, TimeUnit.MINUTES);
        response.sendRedirect(authRequest.authorize(state));
    }

    @GetMapping("/callback/github")
    @Operation(summary = "GitHub回调", description = "GitHub授权回调接口，自动区分登录和绑定操作")
    public OAuthCallbackResult githubCallback(
            AuthCallback callback,
            @Parameter(description = "状态参数，用于区分登录和绑定") @RequestParam(required = false) String state) {
        
        // state 非空校验
        if (state == null || state.isEmpty()) {
            throw new BusinessException("缺少授权状态参数，请重新登录");
        }

        // Validate state for login flow (bind flow has its own state format)
        if (!state.startsWith(BIND_STATE_PREFIX)) {
            String stateKey = OAUTH_STATE_PREFIX + state;
            String stateValue = (String) redisTemplate.opsForValue().get(stateKey);
            if (stateValue == null) {
                throw new BusinessException("无效的授权状态，请重新登录");
            }
            redisTemplate.delete(stateKey);
        }

        // Validate state for bind flow (不删除，交给 handleGithubBind 处理)
        if (state.startsWith(BIND_STATE_PREFIX)) {
            String stateKey = OAUTH_STATE_PREFIX + state;
            String stateValue = (String) redisTemplate.opsForValue().get(stateKey);
            if (stateValue == null) {
                throw new BusinessException("无效的绑定状态，请重新绑定");
            }
        }
        
        AuthRequest authRequest = getGithubAuthRequest();
        AuthResponse<AuthUser> response = authRequest.login(callback);
        if (response.getCode() != 2000) {
            throw new BusinessException("GitHub授权失败: " + response.getMsg());
        }
        AuthUser authUser = response.getData();
        
        if (state != null && state.startsWith(BIND_STATE_PREFIX)) {
            return handleGithubBind(authUser, state);
        }
        
        return handleGithubLogin(authUser);
    }

    @PostMapping("/confirm-username")
    @Operation(summary = "确认用户名", description = "用户名冲突时，用户选择或输入新用户名后调用此接口完成注册")
    public OAuthCallbackResult confirmUsername(
            @Parameter(description = "临时标识key") @RequestParam String tempKey,
            @Parameter(description = "用户选择的用户名") @RequestParam String username) {

        if (username == null || username.length() < 2 || username.length() > 30) {
            throw new BusinessException("用户名长度必须在2-30个字符之间");
        }
        if (!username.matches("^[a-zA-Z0-9_\\u4e00-\\u9fa5]+$")) {
            throw new BusinessException("用户名只能包含字母、数字、下划线和中文");
        }

        String redisKey = OAUTH_TEMP_KEY_PREFIX + tempKey;
        String tempData = redisTemplate.opsForValue().get(redisKey);
        
        if (tempData == null) {
            throw new BusinessException("临时数据已过期，请重新登录");
        }
        
        try {
            GitHubTempUser tempUser = objectMapper.readValue(tempData, GitHubTempUser.class);
            
            if (usersService.getByUsername(username) != null) {
                throw new BusinessException("用户名已被使用，请选择其他用户名");
            }
            
            Users user = usersService.createGithubUser(
                    tempUser.githubId(), 
                    username, 
                    tempUser.email(), 
                    tempUser.avatarUrl()
            );
            
            redisTemplate.delete(redisKey);
            
            log.info("GitHub用户确认用户名并注册成功: githubId={}, username={}", tempUser.githubId(), username);
            
            return loginAndReturnResult(user);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("确认用户名失败: tempKey={}", tempKey, e);
            throw new BusinessException("确认用户名失败，请重试");
        }
    }

    @PostMapping("/unbind/github")
    @Operation(summary = "解绑GitHub账号", description = "解除当前用户与GitHub账号的绑定，需要登录")
    public void unbindGithub() {
        Long userId = StpUtil.getLoginIdAsLong();
        Users user = usersService.getById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        if (user.getGithubId() == null || user.getGithubId().isEmpty()) {
            throw new BusinessException("您还未绑定GitHub账号");
        }
        
        usersService.unbindGithub(userId);
        log.info("用户解绑GitHub成功: userId={}", userId);
    }

    private OAuthCallbackResult loginAndReturnResult(Users user) {
        StpUtil.login(user.getId());
        if (user.getRole() != null) {
            StpUtil.getSession().set("roles",
                    Collections.singletonList(user.getRole().name().toLowerCase()));
        }
        String token = StpUtil.getTokenValue();
        user.setPassword(null);
        usersService.cacheUserInfo(user);
        return OAuthCallbackResult.success(user, token);
    }

    private OAuthCallbackResult handleGithubLogin(AuthUser authUser) {
        String githubId = authUser.getUuid();
        Users user = usersService.getByGithubId(githubId);

        if (user != null) {
            user = usersService.updateGithubUser(user, authUser.getAvatar());
            return loginAndReturnResult(user);
        }

        String baseUsername = authUser.getUsername();
        if (baseUsername == null || baseUsername.isEmpty()) {
            baseUsername = "github_user";
        }
        
        if (usersService.getByUsername(baseUsername) == null) {
            user = usersService.createGithubUser(githubId, baseUsername, authUser.getEmail(), authUser.getAvatar());
            return loginAndReturnResult(user);
        }

        String tempKey = UUID.randomUUID().toString().replace("-", "");
        List<String> suggestedUsernames = generateSuggestedUsernames(baseUsername);
        
        try {
            GitHubTempUser tempUser = new GitHubTempUser(
                    githubId, 
                    authUser.getEmail(), 
                    authUser.getAvatar()
            );
            redisTemplate.opsForValue().set(
                    OAUTH_TEMP_KEY_PREFIX + tempKey, 
                    objectMapper.writeValueAsString(tempUser),
                    OAUTH_TEMP_EXPIRE_TIME, 
                    TimeUnit.MINUTES
            );
            
            log.info("GitHub用户名冲突，等待用户确认: githubId={}, baseUsername={}", githubId, baseUsername);
            
            return OAuthCallbackResult.conflict(
                    tempKey, 
                    baseUsername, 
                    suggestedUsernames, 
                    authUser.getEmail(), 
                    authUser.getAvatar()
            );
        } catch (Exception e) {
            log.error("存储临时GitHub用户数据失败", e);
            throw new BusinessException("登录处理失败，请重试");
        }
    }

    private OAuthCallbackResult handleGithubBind(AuthUser authUser, String state) {
        String stateKey = OAUTH_STATE_PREFIX + state;
        String userIdStr = (String) redisTemplate.opsForValue().getAndDelete(stateKey);

        Long userId;
        try {
            userId = Long.parseLong(userIdStr);
        } catch (NumberFormatException e) {
            throw new BusinessException("无效的绑定请求");
        }
        
        if (!StpUtil.isLogin() || StpUtil.getLoginIdAsLong() != userId) {
            throw new BusinessException("登录状态已变更，请重新绑定");
        }
        
        Users currentUser = usersService.getById(userId);
        if (currentUser == null) {
            throw new BusinessException("用户不存在");
        }
        
        String githubId = authUser.getUuid();
        
        Users existUser = usersService.getByGithubId(githubId);
        if (existUser != null) {
            if (existUser.getId().equals(userId)) {
                return OAuthCallbackResult.success(currentUser, StpUtil.getTokenValue());
            }
            throw new BusinessException("该GitHub账号已被其他用户绑定");
        }
        
        usersService.bindGithub(userId, githubId, authUser.getAvatar());
        
        currentUser = usersService.getById(userId);
        currentUser.setPassword(null);
        
        log.info("用户绑定GitHub成功: userId={}, githubId={}", userId, githubId);
        
        return OAuthCallbackResult.success(currentUser, StpUtil.getTokenValue());
    }

    private List<String> generateSuggestedUsernames(String baseUsername) {
        List<String> suggestions = new ArrayList<>();
        int suffix = 1;
        while (suggestions.size() < 5) {
            String suggested = baseUsername + "_" + suffix;
            if (usersService.getByUsername(suggested) == null) {
                suggestions.add(suggested);
            }
            suffix++;
            if (suffix > 100) break;
        }
        return suggestions;
    }

    private AuthRequest getGithubAuthRequest() {
        if (gitHubOAuthProperties.isDisableSsl()) {
            SslUtils.disableSslVerification();
        }
        return new AuthGithubRequest(AuthConfig.builder()
                .clientId(gitHubOAuthProperties.getClientId())
                .clientSecret(gitHubOAuthProperties.getClientSecret())
                .redirectUri(gitHubOAuthProperties.getRedirectUri())
                .build());
    }

    record GitHubTempUser(String githubId, String email, String avatarUrl) {}
}
