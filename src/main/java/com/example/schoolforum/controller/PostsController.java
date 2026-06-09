package com.example.schoolforum.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckRole;
import cn.dev33.satoken.annotation.SaMode;
import com.example.schoolforum.exception.BusinessException;
import com.example.schoolforum.pojo.Posts;
import com.example.schoolforum.service.PostsService;
import com.example.schoolforum.util.PermissionUtil;
import com.mybatisflex.core.paginate.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/posts")
@RequiredArgsConstructor
@Tag(name = "帖子管理")
@Validated
public class PostsController {

    private final PostsService postsService;

    @PostMapping("/add")
    @SaCheckLogin
    @Operation(summary = "新增帖子")
    public Posts add(
            @Parameter(description = "帖子标题") @RequestParam @NotBlank(message = "标题不能为空") @Size(max = 200, message = "标题长度不能超过200个字符") String title,
            @Parameter(description = "帖子内容（Markdown格式）") @RequestParam @NotBlank(message = "内容不能为空") @Size(max = 50000, message = "内容长度不能超过50000个字符") String content,
            @Parameter(description = "标签ID列表") @RequestParam(required = false) List<Long> tagIds,
            @Parameter(description = "分类ID") @RequestParam(required = false) Long categoryId,
            @Parameter(description = "封面图片URL") @RequestParam(required = false) @Size(max = 500, message = "封面URL长度不能超过500个字符") String coverImage) {
        Long userId = PermissionUtil.getCurrentUserId();
        if (coverImage != null && !coverImage.isBlank() && !coverImage.startsWith("http://") && !coverImage.startsWith("https://")) {
            throw new BusinessException("封面图片URL必须以http://或https://开头");
        }
        return postsService.createPost(userId, title, content, tagIds, categoryId, coverImage);
    }

    @PutMapping("/update/{id}")
    @SaCheckLogin
    @Operation(summary = "更新帖子")
    public Posts update(
            @PathVariable Long id,
            @Parameter(description = "帖子标题") @RequestParam(required = false) @Size(max = 200, message = "标题长度不能超过200个字符") String title,
            @Parameter(description = "帖子内容") @RequestParam(required = false) @Size(max = 50000, message = "内容长度不能超过50000个字符") String content,
            @Parameter(description = "标签ID列表") @RequestParam(required = false) List<Long> tagIds,
            @Parameter(description = "分类ID") @RequestParam(required = false) Long categoryId,
            @Parameter(description = "封面图片URL") @RequestParam(required = false) @Size(max = 500, message = "封面URL长度不能超过500个字符") String coverImage) {
        Long userId = PermissionUtil.getCurrentUserId();
        if (coverImage != null && !coverImage.isBlank() && !coverImage.startsWith("http://") && !coverImage.startsWith("https://")) {
            throw new BusinessException("封面图片URL必须以http://或https://开头");
        }
        return postsService.updatePost(id, title, content, tagIds, categoryId, coverImage, userId);
    }

    @DeleteMapping("/delete/{id}")
    @SaCheckLogin
    @Operation(summary = "删除帖子")
    public void delete(@PathVariable Long id) {
        Long userId = PermissionUtil.getCurrentUserId();
        postsService.deletePost(id, userId);
    }

    @GetMapping("/get/{id}")
    @Operation(summary = "获取帖子详情", description = "noIncrement=true 时只查询不增加浏览量，用于前端轮询刷新")
    public Posts getById(
            @PathVariable Long id,
            @Parameter(description = "是否不增加浏览量，默认false（增加浏览量）") 
            @RequestParam(defaultValue = "false") boolean noIncrement) {
        return postsService.getPostWithViewCount(id, !noIncrement);
    }

    @GetMapping("/list/page")
    @SaCheckRole(value = {"admin", "super_admin"}, mode = SaMode.OR)
    @Operation(summary = "分页查询所有帖子", description = "管理员权限，用于后台管理")
    public Page<Posts> listPage(
            @RequestParam(defaultValue = "1") int pageNumber,
            @RequestParam(defaultValue = "10") int pageSize,
            @Parameter(description = "分类ID，不传则查询所有，支持一级分类自动包含子分类") @RequestParam(required = false) Long categoryId) {
        if (pageSize > 100) pageSize = 100;
        return postsService.listByCategory(categoryId, pageNumber, pageSize);
    }

    @GetMapping("/list")
    @Operation(summary = "查询帖子列表", description = "统一查询接口，支持多种排序方式。categoryId支持一级分类自动包含子分类")
    public Page<Posts> list(
            @RequestParam(defaultValue = "1") int pageNumber,
            @RequestParam(defaultValue = "10") int pageSize,
            @Parameter(description = "排序方式：latest-最新（默认）、hot-热门、essential-精华") 
            @RequestParam(defaultValue = "latest") String sort,
            @Parameter(description = "分类ID，不传则查询所有，支持一级分类自动包含子分类") 
            @RequestParam(required = false) Long categoryId) {
        if (pageSize > 100) pageSize = 100;
        return switch (sort.toLowerCase()) {
            case "hot" -> postsService.getHotListPage(pageNumber, pageSize, categoryId);
            case "essential" -> postsService.listEssential(pageNumber, pageSize, categoryId);
            default -> postsService.getLatestListPage(pageNumber, pageSize, categoryId);
        };
    }

    @PostMapping("/{id}/like")
    @Operation(summary = "点赞")
    public void like(@PathVariable Long id) {
        postsService.likePost(id);
    }

    @DeleteMapping("/{id}/unlike")
    @Operation(summary = "取消点赞")
    public void unlike(@PathVariable Long id) {
        postsService.unlikePost(id);
    }

    @PutMapping("/pinned/{id}")
    @SaCheckRole(value = {"admin", "super_admin"}, mode = SaMode.OR)
    @Operation(summary = "设置置顶")
    public void pinned(@PathVariable Long id, @RequestParam boolean pinned) {
        postsService.setPinned(id, pinned);
    }

    @PutMapping("/essential/{id}")
    @SaCheckRole(value = {"admin", "super_admin"}, mode = SaMode.OR)
    @Operation(summary = "设置精华")
    public void essential(@PathVariable Long id, @RequestParam boolean essential) {
        postsService.setEssential(id, essential);
    }

    @GetMapping("/my")
    @SaCheckLogin
    @Operation(summary = "查询当前用户的帖子", description = "分页获取当前登录用户发布的帖子列表")
    public Page<Posts> listMyPosts(
            @Parameter(description = "页码，默认第1页") @RequestParam(defaultValue = "1") int pageNumber,
            @Parameter(description = "每页数量，默认10条") @RequestParam(defaultValue = "10") int pageSize) {
        if (pageSize > 100) pageSize = 100;
        Long userId = PermissionUtil.getCurrentUserId();
        return postsService.listByAuthor(userId, pageNumber, pageSize);
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "查询指定用户的帖子", description = "分页获取指定用户发布的帖子列表")
    public Page<Posts> listUserPosts(
            @Parameter(description = "用户ID") @PathVariable Long userId,
            @Parameter(description = "页码，默认第1页") @RequestParam(defaultValue = "1") int pageNumber,
            @Parameter(description = "每页数量，默认10条") @RequestParam(defaultValue = "10") int pageSize) {
        if (pageSize > 100) pageSize = 100;
        return postsService.listByAuthor(userId, pageNumber, pageSize);
    }

    @GetMapping("/{id}/related")
    @Operation(summary = "获取相关帖子推荐", description = "根据帖子的分类和标签推荐相关帖子，随机排序返回")
    public List<Posts> getRelatedPosts(
            @Parameter(description = "帖子ID") @PathVariable Long id,
            @Parameter(description = "返回数量，默认5条") @RequestParam(defaultValue = "5") int limit) {
        try {
            return postsService.getRelatedPosts(id, limit);
        } catch (Exception e) {
            log.error("获取相关帖子失败: postId={}", id, e);
            return Collections.emptyList();
        }
    }
}
