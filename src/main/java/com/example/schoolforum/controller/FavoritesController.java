package com.example.schoolforum.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckRole;
import cn.dev33.satoken.annotation.SaMode;
import com.example.schoolforum.pojo.Favorites;
import com.example.schoolforum.pojo.Posts;
import com.example.schoolforum.service.FavoritesService;
import com.example.schoolforum.util.PermissionUtil;
import com.mybatisflex.core.paginate.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/favorites")
@RequiredArgsConstructor
@Tag(name = "收藏管理")
public class FavoritesController {

    private final FavoritesService favoritesService;

    @PostMapping("/add/{postId}")
    @SaCheckLogin
    @Operation(summary = "收藏帖子")
    public Favorites addFavorite(@PathVariable Long postId) {
        Long userId = PermissionUtil.getCurrentUserId();
        return favoritesService.addFavorite(userId, postId);
    }

    @DeleteMapping("/remove/{postId}")
    @SaCheckLogin
    @Operation(summary = "取消收藏")
    public void removeFavorite(@PathVariable Long postId) {
        Long userId = PermissionUtil.getCurrentUserId();
        favoritesService.removeFavorite(userId, postId);
    }

    @GetMapping("/check/{postId}")
    @SaCheckLogin
    @Operation(summary = "检查是否已收藏")
    public Map<String, Boolean> checkFavorite(@PathVariable Long postId) {
        Long userId = PermissionUtil.getCurrentUserId();
        boolean isFavorited = favoritesService.isFavorited(userId, postId);
        return Map.of("isFavorited", isFavorited);
    }

    @GetMapping("/list/page")
    @SaCheckRole(value = {"admin", "super_admin"}, mode = SaMode.OR)
    @Operation(summary = "分页查询所有收藏", description = "管理员权限")
    public Page<Favorites> listPage(
            @RequestParam(defaultValue = "1") int pageNumber,
            @RequestParam(defaultValue = "10") int pageSize) {
        return favoritesService.listPage(pageNumber, pageSize);
    }

    @GetMapping("/list")
    @SaCheckLogin
    @Operation(summary = "查询用户收藏列表")
    public Page<Posts> list(
            @Parameter(description = "页码，默认第1页") @RequestParam(defaultValue = "1") int pageNumber,
            @Parameter(description = "每页数量，默认10条") @RequestParam(defaultValue = "10") int pageSize) {
        Long userId = PermissionUtil.getCurrentUserId();
        return favoritesService.list(userId, pageNumber, pageSize);
    }

}
