package com.example.schoolforum.service;

import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.service.IService;
import com.example.schoolforum.pojo.Conversation;
import com.example.schoolforum.pojo.dto.ConversationVO;

import java.util.List;

public interface ConversationService extends IService<Conversation> {

    Conversation getOrCreateConversation(Long userId1, Long userId2);

    List<ConversationVO> getConversationList(Long userId);

    int getTotalUnreadCount(Long userId);

    void incrementUnreadCount(Long conversationId, Long receiverId);

    void clearUnreadCount(Long conversationId, Long userId);

    void deleteConversation(Long conversationId, Long userId);

    void verifyParticipant(Long conversationId, Long userId);

}
