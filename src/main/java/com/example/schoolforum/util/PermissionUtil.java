package com.example.schoolforum.util;

import cn.dev33.satoken.stp.StpUtil;
import com.example.schoolforum.exception.BusinessException;

import java.util.List;

/**
 * 权限工具类
 * 封装 Sa-Token 的权限校验方法
 */
public final class PermissionUtil {

    private PermissionUtil() {
    }

    /**
     * 检查是否为资源所有者或管理员
     * @param authorId 资源作者ID
     * @param errorMessage 错误信息
     */
    public static void checkOwnerOrAdmin(Long authorId, String errorMessage) {
        Long userId = StpUtil.getLoginIdAsLong();
        boolean isAdmin = StpUtil.hasRole("admin") || StpUtil.hasRole("super_admin");
        
        if (!authorId.equals(userId) && !isAdmin) {
            throw new BusinessException(errorMessage);
        }
    }

    /**
     * 判断当前用户是否为管理员（admin 或 super_admin）
     * @return 是否为管理员
     */
    public static boolean isAdmin() {
        return StpUtil.hasRole("admin") || StpUtil.hasRole("super_admin");
    }

    /**
     * 判断当前用户是否为超级管理员
     * @return 是否为超级管理员
     */
    public static boolean isSuperAdmin() {
        return StpUtil.hasRole("super_admin");
    }

    /**
     * 判断指定用户ID是否为当前登录用户
     * @param userId 用户ID
     * @return 是否为当前用户
     */
    public static boolean isCurrentUser(Long userId) {
        return userId != null && StpUtil.getLoginIdAsLong() == userId.longValue();
    }

    /**
     * 获取当前登录用户ID
     * @return 用户ID
     */
    public static Long getCurrentUserId() {
        return Long.valueOf(StpUtil.getLoginIdAsLong());
    }

    /**
     * 获取当前登录用户ID，未登录返回null
     * @return 用户ID或null
     */
    public static Long getCurrentUserIdOrNull() {
        if (!StpUtil.isLogin()) {
            return null;
        }
        return Long.valueOf(StpUtil.getLoginIdAsLong());
    }

    /**
     * 获取当前用户的角色列表
     * @return 角色列表
     */
    public static List<String> getRoleList() {
        return StpUtil.getRoleList();
    }

    /**
     * 校验当前用户是否拥有指定角色（OR 关系）
     * @param roles 角色列表
     */
    public static void checkRoleOr(String... roles) {
        StpUtil.checkRoleOr(roles);
    }

    /**
     * 校验当前用户是否拥有指定角色（AND 关系）
     * @param roles 角色列表
     */
    public static void checkRoleAnd(String... roles) {
        StpUtil.checkRoleAnd(roles);
    }
}
