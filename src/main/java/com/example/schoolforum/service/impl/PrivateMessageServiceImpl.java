package com.example.schoolforum.service.impl;

import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.util.UpdateEntity;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.example.schoolforum.exception.BusinessException;
import com.example.schoolforum.mapper.ConversationMapper;
import com.example.schoolforum.mapper.PrivateMessageMapper;
import com.example.schoolforum.pojo.Conversation;
import com.example.schoolforum.pojo.PrivateMessage;
import com.example.schoolforum.service.ConversationService;
import com.example.schoolforum.service.PrivateMessageService;
import com.example.schoolforum.websocket.PrivateMessageWebSocketHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 私信消息表 服务层实现。
 *
 * @author sugu
 * @since 2026-03-07
 */
@Service
@RequiredArgsConstructor
public class PrivateMessageServiceImpl extends ServiceImpl<PrivateMessageMapper, PrivateMessage> implements PrivateMessageService {

    private final PrivateMessageMapper privateMessageMapper;
    private final ConversationMapper conversationMapper;
    private final ConversationService conversationService;
    private final PrivateMessageWebSocketHandler webSocketHandler;

    @Override
    public Page<PrivateMessage> list(Long conversationId, int page, int size) {
        QueryWrapper wrapper = QueryWrapper.create()
                .where("conversation_id = ?", conversationId)
                .orderBy("created_at", false);
        return privateMessageMapper.paginateWithRelations(page, size, wrapper);
    }

    @Override
    @Transactional
    public PrivateMessage sendMessage(Long senderId, Long receiverId, String content) {
        if (content == null || content.trim().isEmpty()) {
            throw new BusinessException("消息内容不能为空");
        }

        Conversation conversation = conversationService.getOrCreateConversation(senderId, receiverId);

        PrivateMessage message = PrivateMessage.builder()
                .conversationId(conversation.getId())
                .senderId(senderId)
                .receiverId(receiverId)
                .content(content.trim())
                .isRead(false)
                .createdAt(LocalDateTime.now())
                .build();
        privateMessageMapper.insert(message);

        String previewContent = content.length() > 100 ? content.substring(0, 100) : content;
        Conversation update = UpdateEntity.of(Conversation.class, conversation.getId());
        update.setLastMessageId(message.getId());
        update.setLastMessageAt(message.getCreatedAt());
        update.setLastMessageContent(previewContent);
        update.setUser1Deleted(false);
        update.setUser2Deleted(false);
        update.setUpdatedAt(LocalDateTime.now());
        conversationMapper.update(update);

        conversationService.incrementUnreadCount(conversation.getId(), receiverId);

        webSocketHandler.sendMessage(receiverId, message);

        int totalUnread = conversationService.getTotalUnreadCount(receiverId);
        webSocketHandler.sendUnreadCountUpdate(receiverId, totalUnread);

        return message;
    }

    @Override
    public void markMessagesAsRead(Long conversationId, Long userId) {
        PrivateMessage update = UpdateEntity.of(PrivateMessage.class);
        update.setIsRead(true);

        QueryWrapper wrapper = QueryWrapper.create()
                .where("conversation_id = ?", conversationId)
                .and("receiver_id = ?", userId)
                .and("is_read = 0");

        privateMessageMapper.updateByQuery(update, wrapper);

        conversationService.clearUnreadCount(conversationId, userId);
    }

}
