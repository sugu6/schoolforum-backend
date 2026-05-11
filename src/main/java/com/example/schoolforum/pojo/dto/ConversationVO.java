package com.example.schoolforum.pojo.dto;

import com.example.schoolforum.pojo.Users;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "会话列表项")
public class ConversationVO {

    @Schema(description = "会话ID", example = "1")
    private Long id;

    @Schema(description = "对方用户ID", example = "2")
    private Long otherUserId;

    @Schema(description = "对方用户名", example = "张三")
    private String otherUsername;

    @Schema(description = "对方头像URL", example = "https://example.com/avatar.jpg")
    private String otherAvatarUrl;

    @Schema(description = "最后消息内容", example = "你好，在吗？")
    private String lastMessageContent;

    @Schema(description = "最后消息时间")
    private LocalDateTime lastMessageAt;

    @Schema(description = "未读消息数", example = "2")
    private Integer unreadCount;

    public static ConversationVO fromConversation(com.example.schoolforum.pojo.Conversation conversation, Long currentUserId) {
        ConversationVO vo = new ConversationVO();
        vo.setId(conversation.getId());
        vo.setLastMessageContent(conversation.getLastMessageContent());
        vo.setLastMessageAt(conversation.getLastMessageAt());

        boolean isUser1 = conversation.getUser1Id().equals(currentUserId);
        Users otherUser = isUser1 ? conversation.getUser2() : conversation.getUser1();

        if (otherUser != null) {
            vo.setOtherUserId(otherUser.getId());
            vo.setOtherUsername(otherUser.getUsername());
            vo.setOtherAvatarUrl(otherUser.getAvatarUrl());
        } else {
            vo.setOtherUserId(isUser1 ? conversation.getUser2Id() : conversation.getUser1Id());
        }

        vo.setUnreadCount(isUser1 ? conversation.getUser1UnreadCount() : conversation.getUser2UnreadCount());

        return vo;
    }

}
