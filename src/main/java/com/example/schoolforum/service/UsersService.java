package com.example.schoolforum.service;

import com.example.schoolforum.enums.CodeType;
import com.example.schoolforum.enums.Gender;
import com.example.schoolforum.enums.UserRole;
import com.example.schoolforum.pojo.Users;
import com.example.schoolforum.pojo.dto.LoginResponse;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.service.IService;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 用户表 服务层。
 *
 * @author sugu
 * @since 2026-02-17
 */
public interface UsersService extends IService<Users> {

    void sendCaptcha(String email, CodeType codeType);

    boolean verifyCaptcha(String email, CodeType codeType, String captcha);

    String uploadAvatar(Long userId, MultipartFile file);

    Users register(String username, String password, String email, Integer age, Gender gender, String captcha);

    LoginResponse login(String username, String password);

    Users getByUsername(String username);

    Users getByEmail(String email);

    Users getByGithubId(String githubId);

    Users updateUser(Long targetId, String username, String password, String email, Integer age, Gender gender, String bio, UserRole role, boolean isAdmin, boolean isSuperAdmin);

    Page<Users> list(int pageNumber, int pageSize);

    Page<Users> listPage(int pageNumber, int pageSize);

    void changePassword(Long userId, String oldPassword, String newPassword, String captcha);

    void resetPassword(String email, String newPassword, String captcha);

    Users createGithubUser(String githubId, String username, String email, String avatarUrl);

    Users updateGithubUser(Users user, String avatarUrl);

    Users getCachedUserById(Long userId);

    void evictUserCache(Long userId);

    void cacheUserInfo(Users user);

    void bindGithub(Long userId, String githubId, String avatarUrl);

    void unbindGithub(Long userId);

    void updatePrivacy(Long userId, Boolean showFollowing, Boolean showFollowers);
}
