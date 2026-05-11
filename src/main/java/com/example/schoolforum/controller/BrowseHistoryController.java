package com.example.schoolforum.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckRole;
import cn.dev33.satoken.annotation.SaMode;
import com.example.schoolforum.pojo.BrowseHistory;
import com.example.schoolforum.pojo.Posts;
import com.example.schoolforum.service.BrowseHistoryService;
import com.example.schoolforum.util.PermissionUtil;
import com.mybatisflex.core.paginate.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 浏览历史控制器
 *
 * @author sugu
 * @since 2026-05-10
 */
@RestController
@RequestMapping("/browse-history")
@RequiredArgsConstructor
@Tag(name = "浏览历史管理", description = "浏览历史相关接口")
public class BrowseHistoryController {

    private final BrowseHistoryService browseHistoryService;

    @PostMapping("/add/{postId}")
    @SaCheckLogin
    @Operation(summary = "添加浏览历史", description = "记录用户浏览帖子的历史")
    public BrowseHistory addBrowseHistory(
            @Parameter(description = "帖子ID") @PathVariable Long postId) {
        Long userId = PermissionUtil.getCurrentUserId();
        return browseHistoryService.addBrowseHistory(userId, postId);
    }

    @DeleteMapping("/remove/{postId}")
    @SaCheckLogin
    @Operation(summary = "删除浏览历史", description = "删除指定帖子的浏览历史")
    public void removeBrowseHistory(
            @Parameter(description = "帖子ID") @PathVariable Long postId) {
        Long userId = PermissionUtil.getCurrentUserId();
        browseHistoryService.removeBrowseHistory(userId, postId);
    }

    @DeleteMapping("/clear")
    @SaCheckLogin
    @Operation(summary = "清空浏览历史", description = "清空当前用户的所有浏览历史")
    public void clearBrowseHistory() {
        Long userId = PermissionUtil.getCurrentUserId();
        browseHistoryService.clearBrowseHistory(userId);
    }

    @GetMapping("/list")
    @SaCheckLogin
    @Operation(summary = "查询浏览历史", description = "分页获取当前用户的浏览历史")
    public Page<Posts> list(
            @Parameter(description = "页码，默认第1页") @RequestParam(defaultValue = "1") int pageNumber,
            @Parameter(description = "每页数量，默认10条") @RequestParam(defaultValue = "10") int pageSize) {
        Long userId = PermissionUtil.getCurrentUserId();
        return browseHistoryService.list(userId, pageNumber, pageSize);
    }

    @GetMapping("/list/page")
    @SaCheckRole(value = {"admin", "super_admin"}, mode = SaMode.OR)
    @Operation(summary = "分页查询所有浏览历史", description = "管理员权限，获取所有用户的浏览历史")
    public Page<BrowseHistory> listPage(
            @Parameter(description = "页码，默认第1页") @RequestParam(defaultValue = "1") int pageNumber,
            @Parameter(description = "每页数量，默认10条") @RequestParam(defaultValue = "10") int pageSize) {
        return browseHistoryService.listPage(pageNumber, pageSize);
    }
}
