package com.example.schoolforum.pojo;

import com.example.schoolforum.enums.NotificationType;
import com.example.schoolforum.enums.ReadStatus;
import com.example.schoolforum.enums.RelatedType;
import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.RelationManyToOne;
import com.mybatisflex.annotation.Table;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serializable;
import java.time.LocalDateTime;

import java.io.Serial;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("notifications")
@Schema(description = "系统通知实体")
public class Notifications implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id(keyType = KeyType.Auto)
    @Schema(description = "通知ID", example = "1", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private Long id;

    @Schema(description = "接收通知的用户ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long userId;

    @RelationManyToOne(selfField = "userId", targetField = "id")
    @Column(ignore = true)
    @Schema(description = "接收通知的用户信息", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private Users user;

    @Schema(description = "通知类型：COMMENT-评论通知、REPLY-回复通知、LIKE-点赞通知、FOLLOW-关注通知、UNFOLLOW-取关通知、SYSTEM-系统通知、ESSENTIAL-精华通知、PINNED-置顶通知", 
            example = "COMMENT", requiredMode = Schema.RequiredMode.REQUIRED)
    private NotificationType type;

    @Schema(description = "通知标题", example = "有人评论了你的帖子", requiredMode = Schema.RequiredMode.REQUIRED)
    private String title;

    @Schema(description = "通知内容", example = "用户张三评论了你的帖子《如何学习Java》", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private String content;

    @Schema(description = "关联ID（帖子ID/评论ID等）", example = "1", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private Long relatedId;

    @Schema(description = "关联类型：POST-帖子、COMMENT-评论", example = "POST", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private RelatedType relatedType;

    @Schema(description = "发送者ID（系统通知为空）", example = "2", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private Long senderId;

    @RelationManyToOne(selfField = "senderId", targetField = "id")
    @Column(ignore = true)
    @Schema(description = "发送者信息", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private Users sender;

    @Schema(description = "已读状态：UNREAD-未读、READ-已读", example = "UNREAD", requiredMode = Schema.RequiredMode.REQUIRED)
    private ReadStatus isRead;

    @Schema(description = "创建时间", example = "2026-03-15T10:30:00", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private LocalDateTime createdAt;

    @Schema(description = "更新时间", example = "2026-03-15T10:30:00", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private LocalDateTime updatedAt;

}
