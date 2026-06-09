package com.example.schoolforum.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckRole;
import cn.dev33.satoken.annotation.SaMode;
import com.example.schoolforum.pojo.Follows;
import com.example.schoolforum.pojo.Users;
import com.example.schoolforum.service.FollowsService;
import com.example.schoolforum.service.UsersService;
import com.example.schoolforum.util.PermissionUtil;
import com.mybatisflex.core.paginate.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/follows")
@RequiredArgsConstructor
@Tag(name = "关注管理")
public class FollowsController {

    private final FollowsService followsService;
    private final UsersService usersService;

    @PostMapping("/follow/{userId}")
    @SaCheckLogin
    @Operation(summary = "关注用户", description = "关注指定用户")
    public Follows followUser(
            @Parameter(description = "被关注用户ID") @PathVariable Long userId) {
        Long currentUserId = PermissionUtil.getCurrentUserId();
        return followsService.followUser(currentUserId, userId);
    }

    @DeleteMapping("/unfollow/{userId}")
    @SaCheckLogin
    @Operation(summary = "取消关注", description = "取消关注指定用户")
    public void unfollowUser(
            @Parameter(description = "被取消关注用户ID") @PathVariable Long userId) {
        Long currentUserId = PermissionUtil.getCurrentUserId();
        followsService.unfollowUser(currentUserId, userId);
    }

    @GetMapping("/check/{userId}")
    @SaCheckLogin
    @Operation(summary = "检查是否已关注", description = "检查当前用户是否已关注指定用户")
    public Map<String, Boolean> checkFollowing(
            @Parameter(description = "被检查用户ID") @PathVariable Long userId) {
        Long currentUserId = PermissionUtil.getCurrentUserId();
        boolean isFollowing = followsService.isFollowing(currentUserId, userId);
        return Map.of("isFollowing", isFollowing);
    }

    @GetMapping("/following/list/page")
    @SaCheckRole(value = {"admin", "super_admin"}, mode = SaMode.OR)
    @Operation(summary = "分页查询所有关注关系", description = "管理员权限")
    public Page<Follows> listPageFollowing(
            @RequestParam(defaultValue = "1") int pageNumber,
            @RequestParam(defaultValue = "10") int pageSize) {
        if (pageSize > 100) pageSize = 100;
        return followsService.listPageFollowing(pageNumber, pageSize);
    }

    @GetMapping("/following/{userId}/list")
    @Operation(summary = "查询关注列表", description = "分页获取指定用户关注的用户列表，受隐私设置限制")
    public Page<Users> listFollowing(
            @Parameter(description = "用户ID") @PathVariable Long userId,
            @Parameter(description = "页码，默认第1页") @RequestParam(defaultValue = "1") int pageNumber,
            @Parameter(description = "每页数量，默认10条") @RequestParam(defaultValue = "10") int pageSize) {
        if (pageSize > 100) pageSize = 100;
        if (!checkPrivacyPermission(userId, "following")) {
            Page<Users> emptyPage = new Page<>(pageNumber, pageSize);
            emptyPage.setTotalRow(0);
            return emptyPage;
        }
        return followsService.listFollowing(userId, pageNumber, pageSize);
    }

    @GetMapping("/followers/list/page")
    @SaCheckRole(value = {"admin", "super_admin"}, mode = SaMode.OR)
    @Operation(summary = "分页查询所有粉丝关系", description = "管理员权限")
    public Page<Follows> listPageFollowers(
            @RequestParam(defaultValue = "1") int pageNumber,
            @RequestParam(defaultValue = "10") int pageSize) {
        if (pageSize > 100) pageSize = 100;
        return followsService.listPageFollowers(pageNumber, pageSize);
    }

    @GetMapping("/followers/{userId}/list")
    @Operation(summary = "查询粉丝列表", description = "分页获取关注指定用户的粉丝列表，受隐私设置限制")
    public Page<Users> listFollowers(
            @Parameter(description = "用户ID") @PathVariable Long userId,
            @Parameter(description = "页码，默认第1页") @RequestParam(defaultValue = "1") int pageNumber,
            @Parameter(description = "每页数量，默认10条") @RequestParam(defaultValue = "10") int pageSize) {
        if (pageSize > 100) pageSize = 100;
        if (!checkPrivacyPermission(userId, "followers")) {
            Page<Users> emptyPage = new Page<>(pageNumber, pageSize);
            emptyPage.setTotalRow(0);
            return emptyPage;
        }
        return followsService.listFollowers(userId, pageNumber, pageSize);
    }

    private boolean checkPrivacyPermission(Long userId, String type) {
        Long currentUserId = PermissionUtil.getCurrentUserIdOrNull();
        if (currentUserId != null && currentUserId.equals(userId)) {
            return true;
        }
        Users targetUser = usersService.getById(userId);
        if (targetUser == null) {
            return true;
        }
        if ("following".equals(type)) {
            return !Boolean.FALSE.equals(targetUser.getShowFollowing());
        }
        return !Boolean.FALSE.equals(targetUser.getShowFollowers());
    }
}
