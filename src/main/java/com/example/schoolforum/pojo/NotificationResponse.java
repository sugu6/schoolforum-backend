package com.example.schoolforum.pojo;

import com.example.schoolforum.enums.NotificationType;
import com.example.schoolforum.enums.ReadStatus;
import com.example.schoolforum.enums.RelatedType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "通知响应")
public class NotificationResponse implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "通知ID", example = "1")
    private Long id;

    @Schema(description = "通知类型", example = "COMMENT")
    private NotificationType type;

    @Schema(description = "通知标题", example = "有人评论了你的帖子")
    private String title;

    @Schema(description = "通知内容", example = "用户张三评论了你的帖子《如何学习Java》")
    private String content;

    @Schema(description = "关联ID（帖子ID/评论ID等）", example = "1")
    private Long relatedId;

    @Schema(description = "关联类型", example = "POST")
    private RelatedType relatedType;

    @Schema(description = "发送者ID（系统通知为空）", example = "2")
    private Long senderId;

    @Schema(description = "发送者用户名", example = "张三")
    private String senderName;

    @Schema(description = "发送者头像URL", example = "https://example.com/avatar.jpg")
    private String senderAvatar;

    @Schema(description = "是否已读", example = "UNREAD")
    private ReadStatus isRead;

    @Schema(description = "创建时间", example = "2026-03-15T10:30:00")
    private LocalDateTime createdAt;

    public static NotificationResponse from(Notifications notification) {
        NotificationResponseBuilder builder = NotificationResponse.builder()
                .id(notification.getId())
                .type(notification.getType())
                .title(notification.getTitle())
                .content(notification.getContent())
                .relatedId(notification.getRelatedId())
                .relatedType(notification.getRelatedType())
                .senderId(notification.getSenderId())
                .isRead(notification.getIsRead())
                .createdAt(notification.getCreatedAt());
        
        if (notification.getSender() != null) {
            builder.senderName(notification.getSender().getUsername())
                    .senderAvatar(notification.getSender().getAvatarUrl());
        }
        
        return builder.build();
    }
}
