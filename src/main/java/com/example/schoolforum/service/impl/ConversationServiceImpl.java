package com.example.schoolforum.service.impl;

import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.util.UpdateEntity;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.example.schoolforum.exception.BusinessException;
import com.example.schoolforum.mapper.ConversationMapper;
import com.example.schoolforum.pojo.Conversation;
import com.example.schoolforum.pojo.dto.ConversationVO;
import com.example.schoolforum.service.ConversationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 私信会话表 服务层实现。
 *
 * @author sugu
 * @since 2026-03-07
 */
@Service
@RequiredArgsConstructor
public class ConversationServiceImpl extends ServiceImpl<ConversationMapper, Conversation> implements ConversationService {

    private final ConversationMapper conversationMapper;

    @Override
    public Conversation getOrCreateConversation(Long userId1, Long userId2) {
        if (userId1.equals(userId2)) {
            throw new BusinessException("不能与自己创建会话");
        }

        Long smallerId = Math.min(userId1, userId2);
        Long largerId = Math.max(userId1, userId2);

        Conversation conversation = conversationMapper.selectOneByQuery(
                QueryWrapper.create()
                        .where("user1_id = ?", smallerId)
                        .and("user2_id = ?", largerId)
        );

        if (conversation == null) {
            conversation = Conversation.builder()
                    .user1Id(smallerId)
                    .user2Id(largerId)
                    .user1UnreadCount(0)
                    .user2UnreadCount(0)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();
            conversationMapper.insert(conversation);
        }

        return conversation;
    }

    @Override
    public List<ConversationVO> getConversationList(Long userId) {
        QueryWrapper wrapper = QueryWrapper.create()
                .where("(user1_id = ? AND user1_deleted = ?)", userId, false)
                .or("(user2_id = ? AND user2_deleted = ?)", userId, false)
                .orderBy("last_message_at", false);

        List<Conversation> conversations = conversationMapper.selectListWithRelationsByQuery(wrapper);

        List<ConversationVO> result = new ArrayList<>();
        for (Conversation conversation : conversations) {
            result.add(ConversationVO.fromConversation(conversation, userId));
        }

        return result;
    }

    @Override
    public int getTotalUnreadCount(Long userId) {
        List<Conversation> conversations = conversationMapper.selectListByQuery(
                QueryWrapper.create()
                        .where("user1_id = ?", userId)
                        .or("user2_id = ?", userId)
        );

        int total = 0;
        for (Conversation conversation : conversations) {
            if (conversation.getUser1Id().equals(userId)) {
                total += conversation.getUser1UnreadCount() != null ? conversation.getUser1UnreadCount() : 0;
            } else {
                total += conversation.getUser2UnreadCount() != null ? conversation.getUser2UnreadCount() : 0;
            }
        }

        return total;
    }

    @Override
    public void incrementUnreadCount(Long conversationId, Long receiverId) {
        Conversation conversation = conversationMapper.selectOneById(conversationId);
        if (conversation == null) {
            return;
        }

        Conversation update = UpdateEntity.of(Conversation.class, conversationId);
        update.setUpdatedAt(LocalDateTime.now());

        if (conversation.getUser1Id().equals(receiverId)) {
            int currentCount = conversation.getUser1UnreadCount() != null ? conversation.getUser1UnreadCount() : 0;
            update.setUser1UnreadCount(currentCount + 1);
        } else {
            int currentCount = conversation.getUser2UnreadCount() != null ? conversation.getUser2UnreadCount() : 0;
            update.setUser2UnreadCount(currentCount + 1);
        }

        conversationMapper.update(update);
    }

    @Override
    public void clearUnreadCount(Long conversationId, Long userId) {
        Conversation conversation = conversationMapper.selectOneById(conversationId);
        if (conversation == null) {
            return;
        }

        Conversation update = UpdateEntity.of(Conversation.class, conversationId);
        update.setUpdatedAt(LocalDateTime.now());

        if (conversation.getUser1Id().equals(userId)) {
            update.setUser1UnreadCount(0);
        } else {
            update.setUser2UnreadCount(0);
        }

        conversationMapper.update(update);
    }

    @Override
    public void deleteConversation(Long conversationId, Long userId) {
        Conversation conversation = conversationMapper.selectOneById(conversationId);
        if (conversation == null) {
            throw new BusinessException("会话不存在");
        }

        if (!conversation.getUser1Id().equals(userId) && !conversation.getUser2Id().equals(userId)) {
            throw new BusinessException("无权删除此会话");
        }

        Conversation update = UpdateEntity.of(Conversation.class, conversationId);
        update.setUpdatedAt(LocalDateTime.now());

        if (conversation.getUser1Id().equals(userId)) {
            update.setUser1Deleted(true);
        } else {
            update.setUser2Deleted(true);
        }

        conversationMapper.update(update);
    }

}
