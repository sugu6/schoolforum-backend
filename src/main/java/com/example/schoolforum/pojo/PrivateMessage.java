package com.example.schoolforum.pojo;

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
@Table("private_messages")
@Schema(description = "私信消息实体")
public class PrivateMessage implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id(keyType = KeyType.Auto)
    @Schema(description = "消息ID", example = "1")
    private Long id;

    @Schema(description = "会话ID", example = "1")
    private Long conversationId;

    @RelationManyToOne(selfField = "conversationId", targetField = "id")
    @Column(ignore = true)
    @Schema(description = "会话")
    private Conversation conversation;

    @Schema(description = "发送者ID", example = "1")
    private Long senderId;

    @RelationManyToOne(selfField = "senderId", targetField = "id")
    @Column(ignore = true)
    @Schema(description = "发送者")
    private Users sender;

    @Schema(description = "接收者ID", example = "2")
    private Long receiverId;

    @RelationManyToOne(selfField = "receiverId", targetField = "id")
    @Column(ignore = true)
    @Schema(description = "接收者")
    private Users receiver;

    @Schema(description = "消息内容", example = "你好，在吗？")
    private String content;

    @Schema(description = "是否已读", example = "false")
    private Boolean isRead;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

}
