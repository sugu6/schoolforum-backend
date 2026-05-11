package com.example.schoolforum.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.example.schoolforum.pojo.Notifications;
import com.example.schoolforum.pojo.dto.UnreadCountVO;
import com.example.schoolforum.service.NotificationsService;
import com.example.schoolforum.util.PermissionUtil;
import com.example.schoolforum.util.SseEmitterManager;
import com.mybatisflex.core.paginate.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
@Tag(name = "通知管理")
public class NotificationsController {

    private final NotificationsService notificationsService;
    private final SseEmitterManager sseEmitterManager;

    @GetMapping(value = "/subscribe", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "订阅通知", description = "建立 SSE 连接，实时接收通知")
    public SseEmitter subscribe() {
        if (!cn.dev33.satoken.stp.StpUtil.isLogin()) {
            throw new com.example.schoolforum.exception.BusinessException("请先登录");
        }
        Long userId = PermissionUtil.getCurrentUserId();
        return sseEmitterManager.createEmitter(userId);
    }

    @GetMapping("/list")
    @SaCheckLogin
    @Operation(summary = "获取通知列表", description = "获取当前用户的所有通知（不分页，数据量少时使用）")
    public List<Notifications> list() {
        Long userId = PermissionUtil.getCurrentUserId();
        return notificationsService.listByUserId(userId);
    }

    @GetMapping("/list/page")
    @SaCheckLogin
    @Operation(summary = "分页查询通知列表", description = "分页获取当前用户的通知")
    public Page<Notifications> page(
            @Parameter(description = "页码，默认第1页") @RequestParam(defaultValue = "1") int pageNumber,
            @Parameter(description = "每页数量，默认10条") @RequestParam(defaultValue = "10") int pageSize) {
        Long userId = PermissionUtil.getCurrentUserId();
        return notificationsService.list(userId, pageNumber, pageSize);
    }

    @GetMapping("/unread-count")
    @SaCheckLogin
    @Operation(summary = "获取未读通知数量", description = "获取当前用户的未读通知数量")
    public UnreadCountVO getUnreadCount() {
        Long userId = PermissionUtil.getCurrentUserId();
        long count = notificationsService.getUnreadCount(userId);
        return UnreadCountVO.builder().unreadCount(count).build();
    }

    @PutMapping("/read/{id}")
    @SaCheckLogin
    @Operation(summary = "标记通知为已读", description = "将指定通知标记为已读")
    public void markAsRead(@Parameter(description = "通知ID") @PathVariable Long id) {
        Long userId = PermissionUtil.getCurrentUserId();
        notificationsService.markAsRead(id, userId);
    }

    @PutMapping("/read-all")
    @SaCheckLogin
    @Operation(summary = "标记所有通知为已读", description = "将当前用户的所有通知标记为已读")
    public void markAllAsRead() {
        Long userId = PermissionUtil.getCurrentUserId();
        notificationsService.markAllAsRead(userId);
    }

    @DeleteMapping("/delete/{id}")
    @SaCheckLogin
    @Operation(summary = "删除通知", description = "删除指定的通知")
    public void delete(@Parameter(description = "通知ID") @PathVariable Long id) {
        Long userId = PermissionUtil.getCurrentUserId();
        notificationsService.deleteNotification(id, userId);
    }
}
