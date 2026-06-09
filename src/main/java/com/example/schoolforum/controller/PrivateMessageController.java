package com.example.schoolforum.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.stp.StpUtil;
import com.example.schoolforum.pojo.PrivateMessage;
import com.example.schoolforum.pojo.dto.ConversationVO;
import com.example.schoolforum.pojo.dto.SendMessageRequest;
import com.example.schoolforum.pojo.dto.UnreadCountVO;
import com.example.schoolforum.service.ConversationService;
import com.example.schoolforum.service.PrivateMessageService;
import com.mybatisflex.core.paginate.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/messages")
@RequiredArgsConstructor
@Tag(name = "私信管理")
public class PrivateMessageController {

    private final PrivateMessageService privateMessageService;
    private final ConversationService conversationService;

    @PostMapping("/send")
    @SaCheckLogin
    @Operation(summary = "发送私信", description = "向指定用户发送私信")
    public PrivateMessage sendMessage(@Valid @RequestBody SendMessageRequest request) {
        Long senderId = StpUtil.getLoginIdAsLong();
        return privateMessageService.sendMessage(senderId, request.getReceiverId(), request.getContent());
    }

    @GetMapping("/conversations")
    @SaCheckLogin
    @Operation(summary = "获取会话列表", description = "获取当前用户的所有会话列表")
    public List<ConversationVO> getConversationList() {
        Long userId = StpUtil.getLoginIdAsLong();
        return conversationService.getConversationList(userId);
    }

    @GetMapping("/history/{userId}")
    @SaCheckLogin
    @Operation(summary = "获取与某用户的消息记录", description = "分页获取与指定用户的聊天记录")
    public Page<PrivateMessage> getMessageHistory(
            @Parameter(description = "对方用户ID") @PathVariable Long userId,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "20") int size) {
        Long currentUserId = StpUtil.getLoginIdAsLong();
        var conversation = conversationService.getOrCreateConversation(currentUserId, userId);
        Page<PrivateMessage> messages = privateMessageService.list(conversation.getId(), page, size);
        privateMessageService.markMessagesAsRead(conversation.getId(), currentUserId);
        return messages;
    }

    @GetMapping("/unread-count")
    @SaCheckLogin
    @Operation(summary = "获取未读消息总数", description = "获取当前用户的未读私信总数")
    public UnreadCountVO getUnreadCount() {
        Long userId = StpUtil.getLoginIdAsLong();
        int count = conversationService.getTotalUnreadCount(userId);
        return UnreadCountVO.builder().unreadCount((long) count).build();
    }

    @PutMapping("/read/{conversationId}")
    @SaCheckLogin
    @Operation(summary = "标记会话消息已读", description = "将指定会话的所有消息标记为已读")
    public void markAsRead(@Parameter(description = "会话ID") @PathVariable Long conversationId) {
        Long userId = StpUtil.getLoginIdAsLong();
        conversationService.verifyParticipant(conversationId, userId);
        privateMessageService.markMessagesAsRead(conversationId, userId);
    }

    @DeleteMapping("/conversations/{conversationId}")
    @SaCheckLogin
    @Operation(summary = "删除会话", description = "删除指定的会话（软删除，仅对当前用户隐藏）")
    public void deleteConversation(@Parameter(description = "会话ID") @PathVariable Long conversationId) {
        Long userId = StpUtil.getLoginIdAsLong();
        conversationService.deleteConversation(conversationId, userId);
    }

}
