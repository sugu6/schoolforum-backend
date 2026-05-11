package com.example.schoolforum.service;

import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.service.IService;
import com.example.schoolforum.pojo.PrivateMessage;

public interface PrivateMessageService extends IService<PrivateMessage> {

    Page<PrivateMessage> list(Long conversationId, int page, int size);

    PrivateMessage sendMessage(Long senderId, Long receiverId, String content);

    void markMessagesAsRead(Long conversationId, Long userId);

}
