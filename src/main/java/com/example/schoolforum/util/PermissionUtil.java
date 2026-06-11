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

    /**
     * 校验用户是否有权操作指定资源（资源不存在或无权均抛出异常）
     * @param resourceOwnerId 资源所属者ID
     * @param currentUserId 当前用户ID
     * @param resourceName 资源名称（用于错误消息）
     */
    public static void checkOwnership(Long resourceOwnerId, Long currentUserId, String resourceName) {
        checkOwnership(resourceOwnerId, currentUserId, resourceName, "无权操作此");
    }

    /**
     * 校验用户是否有权操作指定资源
     * @param resourceOwnerId 资源所属者ID
     * @param currentUserId 当前用户ID
     * @param resourceName 资源名称（用于错误消息）
     * @param action 操作描述（如"无权删除此"、"无权修改此"）
     */
    public static void checkOwnership(Long resourceOwnerId, Long currentUserId, String resourceName, String action) {
        if (resourceOwnerId == null) {
            throw new BusinessException(resourceName + "不存在");
        }
        if (!resourceOwnerId.equals(currentUserId)) {
            throw new BusinessException(action + resourceName);
        }
    }

    /**
     * 校验用户是否在指定的用户列表中有权限操作（适用于多方共享资源）
     * @param authorizedUsers 有权限的用户ID列表
     * @param currentUserId 当前用户ID
     * @param resourceName 资源名称（用于错误消息）
     */
    public static void checkMultiUserPermission(Long[] authorizedUsers, Long currentUserId, String resourceName) {
        if (authorizedUsers == null || authorizedUsers.length == 0) {
            throw new BusinessException(resourceName + "不存在");
        }
        for (Long authorizedUserId : authorizedUsers) {
            if (authorizedUserId != null && authorizedUserId.equals(currentUserId)) {
                return;
            }
        }
        throw new BusinessException("无权操作此" + resourceName);
    }
}
