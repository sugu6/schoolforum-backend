package com.example.schoolforum.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import cn.dev33.satoken.annotation.SaMode;
import com.example.schoolforum.enums.ActiveStatus;
import com.example.schoolforum.pojo.Tags;
import com.example.schoolforum.service.TagsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/tags")
@RequiredArgsConstructor
@Tag(name = "标签管理")
@Validated
public class TagsController {

    private final TagsService tagsService;

    @GetMapping("/list")
    @SaCheckRole(value = {"admin", "super_admin"}, mode = SaMode.OR)
    @Operation(summary = "获取所有标签", description = "管理员权限，返回所有标签（含禁用）")
    public List<Tags> listAll() {
        return tagsService.listAll();
    }

    @GetMapping("/list/enabled")
    @Operation(summary = "获取启用的标签")
    public List<Tags> listEnabled() {
        return tagsService.listEnabled();
    }

    @GetMapping("/list/category/{categoryId}")
    @Operation(summary = "按分类获取标签", description = "获取指定分类下的标签列表")
    public List<Tags> listByCategory(@PathVariable Long categoryId) {
        return tagsService.listByCategoryId(categoryId);
    }

    @GetMapping("/list/hot")
    @Operation(summary = "热门标签", description = "按帖子数量排序获取热门标签")
    public List<Tags> getHotTags(
            @Parameter(description = "返回数量，默认10") @RequestParam(defaultValue = "10") int limit) {
        return tagsService.getHotTags(limit);
    }

    @GetMapping("/get/{id}")
    @Operation(summary = "获取标签详情")
    public Tags getById(@PathVariable Long id) {
        Tags tag = tagsService.getById(id);
        if (tag == null) {
            throw new IllegalArgumentException("标签不存在");
        }
        return tag;
    }

    @PostMapping("/add")
    @SaCheckRole(value = {"admin", "super_admin"}, mode = SaMode.OR)
    @Operation(summary = "新增标签", description = "管理员权限")
    public Tags add(
            @Parameter(description = "标签名称") @RequestParam @NotBlank(message = "标签名称不能为空") @Size(max = 50, message = "标签名称长度不能超过50个字符") String name,
            @Parameter(description = "关联分类ID（推荐分类）") @RequestParam(required = false) Long categoryId) {
        return tagsService.createTag(name, categoryId);
    }

    @PutMapping("/update/{id}")
    @SaCheckRole(value = {"admin", "super_admin"}, mode = SaMode.OR)
    @Operation(summary = "更新标签", description = "管理员权限")
    public Tags update(
            @PathVariable Long id,
            @Parameter(description = "标签名称") @RequestParam(required = false) @Size(max = 50, message = "标签名称长度不能超过50个字符") String name,
            @Parameter(description = "关联分类ID") @RequestParam(required = false) Long categoryId,
            @Parameter(description = "状态：ACTIVE-启用，INACTIVE-禁用") @RequestParam(required = false) ActiveStatus status) {
        return tagsService.updateTag(id, name, categoryId, status);
    }

    @DeleteMapping("/delete/{id}")
    @SaCheckRole(value = {"admin", "super_admin"}, mode = SaMode.OR)
    @Operation(summary = "删除标签", description = "管理员权限")
    public void delete(@PathVariable Long id) {
        tagsService.deleteTag(id);
    }
}
