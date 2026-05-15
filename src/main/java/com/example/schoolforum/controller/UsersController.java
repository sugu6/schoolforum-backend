package com.example.schoolforum.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckRole;
import cn.dev33.satoken.annotation.SaMode;
import cn.dev33.satoken.stp.StpUtil;
import com.example.schoolforum.enums.ActiveStatus;
import com.example.schoolforum.enums.CodeType;
import com.example.schoolforum.enums.Gender;
import com.example.schoolforum.enums.UserRole;
import com.example.schoolforum.exception.BusinessException;
import com.example.schoolforum.pojo.AccountDeletionRequest;
import com.example.schoolforum.pojo.Users;
import com.example.schoolforum.pojo.dto.LoginResponse;
import com.example.schoolforum.pojo.dto.UserSearchDocument;
import com.example.schoolforum.service.AccountDeletionService;
import com.example.schoolforum.service.SearchService;
import com.example.schoolforum.service.UsersService;
import com.example.schoolforum.util.PermissionUtil;
import com.mybatisflex.core.paginate.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@Tag(name = "用户管理", description = "用户相关接口，包括用户注册、登录、信息管理等")
public class UsersController {

    private final UsersService usersService;
    private final SearchService searchService;
    private final AccountDeletionService accountDeletionService;
    private final PasswordEncoder passwordEncoder;

    @GetMapping("captcha")
    @Operation(summary = "发送验证码", description = "发送验证码。注册和重置密码需传邮箱，修改密码需登录且无需传邮箱")
    public String sendCaptcha(
            @Parameter(description = "邮箱地址（注册和重置密码必传，修改密码无需传）") @RequestParam(required = false) String email,
            @Parameter(description = "验证码类型", required = true, schema = @Schema(allowableValues = {"register", "changePassword", "resetPassword"}))
            @RequestParam String type) {
        CodeType codeType = switch (type) {
            case "register" -> CodeType.REGISTER;
            case "changePassword" -> CodeType.CHANGE_PASSWORD;
            case "resetPassword" -> CodeType.RESET_PASSWORD;
            default -> throw new BusinessException("无效的验证码类型");
        };

        String targetEmail = email;
        if (codeType == CodeType.CHANGE_PASSWORD) {
            Long userId = PermissionUtil.getCurrentUserId();
            Users user = usersService.getById(userId);
            if (user == null) {
                throw new BusinessException("用户不存在");
            }
            targetEmail = user.getEmail();
        } else if (email == null || email.isEmpty()) {
            throw new BusinessException("邮箱不能为空");
        }

        usersService.sendCaptcha(targetEmail, codeType);
        return "验证码已发送";
    }

    @PostMapping("add")
    @Operation(summary = "新增用户", description = "新增用户")
    @SaCheckRole(value = {"admin", "super_admin"}, mode = SaMode.OR)
    public Users add(
            @Parameter(description = "用户名") @RequestParam String username,
            @Parameter(description = "密码") @RequestParam String password,
            @Parameter(description = "邮箱") @RequestParam(required = false) String email,
            @Parameter(description = "年龄") @RequestParam(required = false) Integer age,
            @Parameter(description = "性别", schema = @Schema(allowableValues = {"MALE", "FEMALE", "SECRET"}))
            @RequestParam(required = false) String gender) {
        Gender userGender = parseGender(gender);
        Users users = new Users();
        users.setUsername(username);
        users.setPassword(passwordEncoder.encode(password));
        users.setEmail(email);
        users.setAge(age);
        users.setGender(userGender);
        users.setRole(UserRole.USER);
        users.setIsActive(ActiveStatus.INACTIVE);
        users.setShowFollowing(false);
        users.setShowFollowers(false);
        users.setLevel(1);
        users.setExp(0);
        users.setPoints(0);
        users.setContinuousSignDays(0);
        users.setSignCards(0);
        users.setCreatedAt(LocalDateTime.now());
        users.setLastLoginAt(LocalDateTime.now());
        usersService.save(users);
        searchService.indexUser(UserSearchDocument.fromEntity(users));
        users.setPassword(null);
        return users;
    }

    @DeleteMapping("remove/{id}")
    @Operation(summary = "删除用户", description = "根据ID删除用户")
    @SaCheckRole(value = {"admin", "super_admin"}, mode = SaMode.OR)
    public String remove(@Parameter(description = "用户ID") @PathVariable Long id) {
        if (!usersService.removeById(id)) {
            throw new BusinessException("删除失败，用户不存在");
        }
        searchService.deleteUser(id);
        return "删除成功";
    }

    @PutMapping("update")
    @Operation(summary = "更新用户", description = "更新用户信息，普通用户只能更新自己，管理员可更新任意用户和角色")
    @SaCheckLogin
    public Users update(
            @Parameter(description = "用户ID，管理员可指定，普通用户无需传此参数") @RequestParam(required = false) Long id,
            @Parameter(description = "用户名") @RequestParam(required = false) String username,
            @Parameter(description = "密码") @RequestParam(required = false) String password,
            @Parameter(description = "邮箱") @RequestParam(required = false) String email,
            @Parameter(description = "年龄") @RequestParam(required = false) Integer age,
            @Parameter(description = "性别", schema = @Schema(allowableValues = {"MALE", "FEMALE", "SECRET"}))
            @RequestParam(required = false) String gender,
            @Parameter(description = "个人简介") @RequestParam(required = false) String bio,
            @Parameter(description = "角色，仅管理员可修改", schema = @Schema(allowableValues = {"USER", "ADMIN", "SUPER_ADMIN"}))
            @RequestParam(required = false) String role) {
        UserRole userRole = parseUserRole(role);
        Gender userGender = parseGender(gender);

        long currentUserId = PermissionUtil.getCurrentUserId();
        boolean isSuperAdmin = PermissionUtil.isSuperAdmin();
        boolean isAdmin = PermissionUtil.isAdmin();
        Long targetId = ((isSuperAdmin || isAdmin) && id != null) ? id : currentUserId;

        return usersService.updateUser(targetId, username, password, email, age, userGender, bio, userRole, isAdmin, isSuperAdmin);
    }

    @GetMapping("list/page")
    @Operation(summary = "分页查询用户", description = "管理员权限，分页获取用户列表")
    @SaCheckRole(value = {"admin", "super_admin"}, mode = SaMode.OR)
    public Page<Users> listPage(
            @Parameter(description = "页码，默认第1页") @RequestParam(defaultValue = "1") Integer pageNumber,
            @Parameter(description = "每页数量，默认10条") @RequestParam(defaultValue = "10") Integer pageSize) {
        Page<Users> page = usersService.listPage(pageNumber, pageSize);
        page.getRecords().forEach(u -> u.setPassword(null));
        return page;
    }

    @GetMapping("getInfo/{id}")
    @Operation(summary = "获取用户详情", description = "根据ID获取用户详细信息")
    public Users getInfo(@Parameter(description = "用户ID") @PathVariable Long id) {
        Users users = usersService.getCachedUserById(id);
        if (users == null) {
            throw new BusinessException("用户不存在");
        }
        return users;
    }

    @GetMapping("list")
    @Operation(summary = "查询用户", description = "分页获取用户列表")
    public Page<Users> list(
            @Parameter(description = "页码，默认第1页") @RequestParam(defaultValue = "1") Integer pageNumber,
            @Parameter(description = "每页数量，默认10条") @RequestParam(defaultValue = "10") Integer pageSize) {
        Page<Users> page = usersService.list(pageNumber, pageSize);
        page.getRecords().forEach(u -> u.setPassword(null));
        return page;
    }

    @PostMapping("register")
    @Operation(summary = "用户注册", description = "新用户注册接口，需要邮箱验证码")
    public Users register(
            @Parameter(description = "用户名", required = true) @RequestParam String username,
            @Parameter(description = "密码", required = true) @RequestParam String password,
            @Parameter(description = "邮箱", required = true) @RequestParam String email,
            @Parameter(description = "验证码", required = true) @RequestParam String captcha,
            @Parameter(description = "年龄") @RequestParam(required = false) Integer age,
            @Parameter(description = "性别", schema = @Schema(allowableValues = {"MALE", "FEMALE", "SECRET"}))
            @RequestParam(required = false) String gender) {
        Gender userGender = parseGender(gender);
        return usersService.register(username, password, email, age, userGender, captcha);
    }

    @PostMapping("login")
    @Operation(summary = "用户登录", description = "用户登录接口，返回用户信息和Token")
    public LoginResponse login(
            @Parameter(description = "用户名", required = true) @RequestParam String username,
            @Parameter(description = "密码", required = true) @RequestParam String password) {
        return usersService.login(username, password);
    }

    @PostMapping("logout")
    @Operation(summary = "用户登出", description = "用户登出接口")
    public String logout() {
        if (StpUtil.isLogin()) {
            StpUtil.logout();
        }
        return "登出成功";
    }

    @GetMapping("info")
    @Operation(summary = "获取当前用户信息", description = "获取当前登录用户信息")
    @SaCheckLogin
    public Users getCurrentUser() {
        Long userId = PermissionUtil.getCurrentUserId();
        Users users = usersService.getCachedUserById(userId);
        if (users == null) {
            throw new BusinessException("用户不存在");
        }
        return users;
    }

    @PostMapping("changePassword")
    @Operation(summary = "修改密码", description = "修改当前登录用户密码，需要验证码")
    @SaCheckLogin
    public String changePassword(
            @Parameter(description = "旧密码", required = true) @RequestParam String oldPassword,
            @Parameter(description = "新密码", required = true) @RequestParam String newPassword,
            @Parameter(description = "验证码", required = true) @RequestParam String captcha) {
        Long userId = PermissionUtil.getCurrentUserId();
        usersService.changePassword(userId, oldPassword, newPassword, captcha);
        return "密码修改成功";
    }

    @PostMapping("resetPassword")
    @Operation(summary = "重置密码", description = "通过邮箱验证码重置密码")
    public String resetPassword(
            @Parameter(description = "邮箱", required = true) @RequestParam String email,
            @Parameter(description = "新密码", required = true) @RequestParam String newPassword,
            @Parameter(description = "验证码", required = true) @RequestParam String captcha) {
        usersService.resetPassword(email, newPassword, captcha);
        return "密码重置成功";
    }

    @PostMapping("avatar")
    @Operation(summary = "上传头像", description = "上传用户头像，支持jpg、png、gif、webp格式，最大5MB")
    @SaCheckLogin
    public Map<String, String> uploadAvatar(
            @Parameter(description = "头像文件", required = true) @RequestParam("file") MultipartFile file) {
        Long userId = PermissionUtil.getCurrentUserId();
        String avatarUrl = usersService.uploadAvatar(userId, file);
        return Map.of("avatarUrl", avatarUrl);
    }

    @PutMapping("privacy")
    @Operation(summary = "更新隐私设置", description = "更新当前用户的隐私设置，如是否公开关注/粉丝列表")
    @SaCheckLogin
    public String updatePrivacy(
            @Parameter(description = "是否公开关注列表") @RequestParam(required = false) Boolean showFollowing,
            @Parameter(description = "是否公开粉丝列表") @RequestParam(required = false) Boolean showFollowers) {
        Long userId = PermissionUtil.getCurrentUserId();
        usersService.updatePrivacy(userId, showFollowing, showFollowers);
        return "隐私设置更新成功";
    }

    @PostMapping("deletion/request")
    @Operation(summary = "申请账户注销", description = "提交账户注销申请，冷静期7天，期间可撤销，申请后账户自动登出")
    @SaCheckLogin
    public AccountDeletionRequest requestDeletion(
            @Parameter(description = "注销原因") @RequestParam(required = false) String reason) {
        Long userId = PermissionUtil.getCurrentUserId();
        return accountDeletionService.requestDeletion(userId, reason);
    }

    @PostMapping("deletion/cancel")
    @Operation(summary = "撤销账户注销", description = "撤销账户注销申请，冷静期内可撤销")
    @SaCheckLogin
    public String cancelDeletion() {
        Long userId = PermissionUtil.getCurrentUserId();
        accountDeletionService.cancelDeletion(userId);
        return "注销申请已撤销";
    }

    @GetMapping("deletion/status")
    @Operation(summary = "获取注销申请状态", description = "获取当前用户的账户注销申请状态")
    @SaCheckLogin
    public AccountDeletionRequest getDeletionStatus() {
        Long userId = PermissionUtil.getCurrentUserId();
        AccountDeletionRequest request = accountDeletionService.getPendingRequest(userId);
        if (request != null) {
            request.setUserId(null);
        }
        return request;
    }

    private Gender parseGender(String gender) {
        if (gender == null) return null;
        return switch (gender) {
            case "MALE" -> Gender.MALE;
            case "FEMALE" -> Gender.FEMALE;
            case "SECRET" -> Gender.SECRET;
            default -> null;
        };
    }

    private UserRole parseUserRole(String role) {
        if (role == null) return null;
        return switch (role) {
            case "USER" -> UserRole.USER;
            case "ADMIN" -> UserRole.ADMIN;
            case "SUPER_ADMIN" -> UserRole.SUPER_ADMIN;
            default -> null;
        };
    }
}
