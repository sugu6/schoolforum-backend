package com.example.schoolforum.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckRole;
import cn.dev33.satoken.annotation.SaMode;
import com.example.schoolforum.pojo.Announcements;
import com.example.schoolforum.pojo.dto.AnnouncementCreateRequest;
import com.example.schoolforum.pojo.dto.AnnouncementUpdateRequest;
import com.example.schoolforum.service.AnnouncementsService;
import com.example.schoolforum.util.PermissionUtil;
import com.mybatisflex.core.paginate.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/announcements")
@RequiredArgsConstructor
@Tag(name = "公告管理")
public class AnnouncementsController {

    private final AnnouncementsService announcementsService;

    @PostMapping
    @SaCheckLogin
    @SaCheckRole(value = {"admin", "super_admin"}, mode = SaMode.OR)
    @Operation(summary = "创建公告", description = "管理员创建公告，可选择立即发布或保存为草稿")
    public Announcements create(@Valid @RequestBody AnnouncementCreateRequest request) {
        Long publisherId = PermissionUtil.getCurrentUserId();
        return announcementsService.createAnnouncement(request, publisherId);
    }

    @PutMapping("/{id}")
    @SaCheckLogin
    @SaCheckRole(value = {"admin", "super_admin"}, mode = SaMode.OR)
    @Operation(summary = "修改公告", description = "管理员修改公告内容，仅可修改草稿状态的公告")
    public Announcements update(
            @Parameter(description = "公告ID") @PathVariable Long id,
            @Valid @RequestBody AnnouncementUpdateRequest request) {
        return announcementsService.updateAnnouncement(id, request);
    }

    @DeleteMapping("/{id}")
    @SaCheckLogin
    @SaCheckRole(value = {"admin", "super_admin"}, mode = SaMode.OR)
    @Operation(summary = "删除公告", description = "管理员删除公告")
    public void delete(@Parameter(description = "公告ID") @PathVariable Long id) {
        announcementsService.deleteAnnouncement(id);
    }

    @PutMapping("/{id}/publish")
    @SaCheckLogin
    @SaCheckRole(value = {"admin", "super_admin"}, mode = SaMode.OR)
    @Operation(summary = "发布公告", description = "将草稿或已下架的公告发布")
    public Announcements publish(@Parameter(description = "公告ID") @PathVariable Long id) {
        return announcementsService.publishAnnouncement(id);
    }

    @PutMapping("/{id}/offline")
    @SaCheckLogin
    @SaCheckRole(value = {"admin", "super_admin"}, mode = SaMode.OR)
    @Operation(summary = "下架公告", description = "将已发布的公告下架")
    public Announcements offline(@Parameter(description = "公告ID") @PathVariable Long id) {
        return announcementsService.offlineAnnouncement(id);
    }

    @PutMapping("/{id}/top")
    @SaCheckLogin
    @SaCheckRole(value = {"admin", "super_admin"}, mode = SaMode.OR)
    @Operation(summary = "切换置顶状态", description = "切换公告的置顶状态，仅已发布的公告可置顶")
    public void toggleTop(@Parameter(description = "公告ID") @PathVariable Long id) {
        announcementsService.toggleTop(id);
    }

    @GetMapping("/admin/list")
    @SaCheckLogin
    @SaCheckRole(value = {"admin", "super_admin"}, mode = SaMode.OR)
    @Operation(summary = "管理端-获取所有公告", description = "管理员获取所有公告（含草稿和已下架），按创建时间倒序")
    public Page<Announcements> adminList(
            @Parameter(description = "页码，默认第1页") @RequestParam(defaultValue = "1") int pageNumber,
            @Parameter(description = "每页数量，默认10条") @RequestParam(defaultValue = "10") int pageSize) {
        return announcementsService.listAll(pageNumber, pageSize);
    }

    @GetMapping("/list")
    @Operation(summary = "获取已发布公告列表", description = "公开接口，获取已发布的公告列表，置顶优先，按创建时间倒序")
    public Page<Announcements> list(
            @Parameter(description = "页码，默认第1页") @RequestParam(defaultValue = "1") int pageNumber,
            @Parameter(description = "每页数量，默认10条") @RequestParam(defaultValue = "10") int pageSize) {
        return announcementsService.listPublished(pageNumber, pageSize);
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取公告详情", description = "公开接口，获取指定公告的详情")
    public Announcements detail(@Parameter(description = "公告ID") @PathVariable Long id) {
        return announcementsService.getAnnouncementDetail(id);
    }
}
