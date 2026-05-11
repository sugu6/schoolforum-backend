package com.example.schoolforum.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import cn.dev33.satoken.annotation.SaMode;
import com.example.schoolforum.enums.ActiveStatus;
import com.example.schoolforum.pojo.Categories;
import com.example.schoolforum.service.CategoriesService;
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
@RequestMapping("/categories")
@RequiredArgsConstructor
@Tag(name = "分类管理")
@Validated
public class CategoriesController {

    private final CategoriesService categoriesService;

    @GetMapping("/list")
    @Operation(summary = "获取分类列表", description = "获取启用的分类列表（树形结构），包含一级和二级分类")
    public List<Categories> list() {
        return categoriesService.listEnabled();
    }

    @GetMapping("/list/page")
    @SaCheckRole(value = {"admin", "super_admin"}, mode = SaMode.OR)
    @Operation(summary = "分页查询所有分类", description = "管理员权限，返回所有分类（含禁用）")
    public List<Categories> listPage() {
        return categoriesService.listAll();
    }

    @GetMapping("/get/{id}")
    @Operation(summary = "获取分类详情")
    public Categories getById(@PathVariable Long id) {
        Categories category = categoriesService.getById(id);
        if (category == null) {
            throw new IllegalArgumentException("分类不存在");
        }
        return category;
    }

    @PostMapping("/add")
    @SaCheckRole(value = {"admin", "super_admin"}, mode = SaMode.OR)
    @Operation(summary = "新增分类", description = "管理员权限")
    public Categories add(
            @Parameter(description = "分类名称") @RequestParam @NotBlank(message = "分类名称不能为空") @Size(max = 50, message = "分类名称长度不能超过50个字符") String name,
            @Parameter(description = "父分类ID，一级分类不传") @RequestParam(required = false) Long parentId,
            @Parameter(description = "层级：1-一级分类，2-二级分类") @RequestParam(required = false, defaultValue = "1") Integer level) {
        return categoriesService.createCategory(name, parentId, level);
    }

    @PutMapping("/update/{id}")
    @SaCheckRole(value = {"admin", "super_admin"}, mode = SaMode.OR)
    @Operation(summary = "更新分类", description = "管理员权限")
    public Categories update(
            @PathVariable Long id,
            @Parameter(description = "分类名称") @RequestParam(required = false) @Size(max = 50, message = "分类名称长度不能超过50个字符") String name,
            @Parameter(description = "状态：ACTIVE-启用，INACTIVE-禁用") @RequestParam(required = false) ActiveStatus status) {
        return categoriesService.updateCategory(id, name, status);
    }

    @DeleteMapping("/delete/{id}")
    @SaCheckRole(value = {"admin", "super_admin"}, mode = SaMode.OR)
    @Operation(summary = "删除分类", description = "管理员权限")
    public void delete(@PathVariable Long id) {
        categoriesService.deleteCategory(id);
    }
}
