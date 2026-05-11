package com.example.schoolforum.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckRole;
import cn.dev33.satoken.annotation.SaMode;
import com.example.schoolforum.pojo.Comments;
import com.example.schoolforum.pojo.vo.CommentListVO;
import com.example.schoolforum.service.CommentsService;
import com.example.schoolforum.util.PermissionUtil;
import com.mybatisflex.core.paginate.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/comments")
@RequiredArgsConstructor
@Tag(name = "评论管理", description = "评论相关接口")
@Validated
public class CommentsController {

    private final CommentsService commentsService;

    @PostMapping("/add")
    @SaCheckLogin
    @Operation(summary = "新增评论", description = "发表评论或回复")
    public Comments add(
            @Parameter(description = "评论内容") @RequestParam @NotBlank String content,
            @Parameter(description = "帖子ID") @RequestParam Long postId,
            @Parameter(description = "父评论ID") @RequestParam(required = false) Long parentId) {
        return commentsService.addComment(PermissionUtil.getCurrentUserId(), postId, parentId, content);
    }

    @PutMapping("/update/{id}")
    @SaCheckLogin
    @Operation(summary = "更新评论")
    public Comments update(
            @PathVariable Long id,
            @RequestParam @NotBlank String content) {
        return commentsService.updateComment(id, content, PermissionUtil.getCurrentUserId());
    }

    @DeleteMapping("/delete/{id}")
    @SaCheckLogin
    @Operation(summary = "删除评论")
    public String delete(@PathVariable Long id) {
        return commentsService.deleteComment(id, PermissionUtil.getCurrentUserId());
    }

    @GetMapping("/get/{id}")
    @Operation(summary = "获取评论详情")
    public Comments getById(@PathVariable Long id) {
        return commentsService.getCommentById(id);
    }

    @GetMapping("/list/post/{postId}")
    @Operation(summary = "获取帖子评论", description = "返回树形结构，包含 total 总数")
    public CommentListVO listByPostId(@PathVariable Long postId) {
        return commentsService.listByPostId(postId);
    }

    @GetMapping("/list/page")
    @SaCheckRole(value = {"admin", "super_admin"}, mode = SaMode.OR)
    @Operation(summary = "分页查询所有评论", description = "管理员权限")
    public Page<Comments> listPage(
            @RequestParam(defaultValue = "1") int pageNumber,
            @RequestParam(defaultValue = "10") int pageSize) {
        return commentsService.listPage(pageNumber, pageSize);
    }

    @GetMapping("/list")
    @Operation(summary = "查询评论")
    public Page<Comments> list(
            @RequestParam(defaultValue = "1") int pageNumber,
            @RequestParam(defaultValue = "10") int pageSize) {
        return commentsService.list(pageNumber, pageSize);
    }

    @PostMapping("/{id}/like")
    @Operation(summary = "点赞评论")
    public void like(@PathVariable Long id) {
        commentsService.likeComment(id);
    }

    @DeleteMapping("/{id}/unlike")
    @Operation(summary = "取消点赞评论")
    public void unlike(@PathVariable Long id) {
        commentsService.unlikeComment(id);
    }

    @GetMapping("/list/hot/post/{postId}")
    @Operation(summary = "获取热门评论")
    public List<Comments> listHot(
            @PathVariable Long postId,
            @RequestParam(defaultValue = "10") int limit) {
        return commentsService.listHotByPostId(postId, limit);
    }
}
