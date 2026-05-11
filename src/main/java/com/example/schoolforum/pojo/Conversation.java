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
@Table("conversations")
@Schema(description = "私信会话实体")
public class Conversation implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id(keyType = KeyType.Auto)
    @Schema(description = "会话ID", example = "1")
    private Long id;

    @Schema(description = "用户1 ID（较小的用户ID）", example = "1")
    private Long user1Id;

    @RelationManyToOne(selfField = "user1Id", targetField = "id")
    @Column(ignore = true)
    @Schema(description = "用户1")
    private Users user1;

    @Schema(description = "用户2 ID（较大的用户ID）", example = "2")
    private Long user2Id;

    @RelationManyToOne(selfField = "user2Id", targetField = "id")
    @Column(ignore = true)
    @Schema(description = "用户2")
    private Users user2;

    @Schema(description = "最后一条消息ID", example = "100")
    private Long lastMessageId;

    @Schema(description = "最后消息时间")
    private LocalDateTime lastMessageAt;

    @Schema(description = "最后消息内容预览", example = "你好，在吗？")
    private String lastMessageContent;

    @Schema(description = "用户1未读消息数", example = "0")
    private Integer user1UnreadCount;

    @Schema(description = "用户2未读消息数", example = "2")
    private Integer user2UnreadCount;

    @Schema(description = "用户1是否删除该会话", example = "false")
    private Boolean user1Deleted;

    @Schema(description = "用户2是否删除该会话", example = "false")
    private Boolean user2Deleted;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;

}
