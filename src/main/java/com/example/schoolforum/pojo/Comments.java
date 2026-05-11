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
@Table("comments")
@Schema(description = "评论实体")
public class Comments implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id(keyType = KeyType.Auto)
    @Schema(description = "评论ID", example = "1")
    private Long id;

    @Schema(description = "评论作者ID", example = "1")
    private Long authorId;

    @Schema(description = "评论内容", example = "这篇文章写得很好！")
    private String content;

    @Schema(description = "父评论ID，用于回复评论", example = "0")
    private Long parentId;

    @RelationManyToOne(selfField = "parentId", targetField = "id")
    @Column(ignore = true)
    @Schema(description = "父评论")
    private Comments parent;

    @Schema(description = "所属帖子ID", example = "1")
    private Long postId;

    @RelationManyToOne(selfField = "postId", targetField = "id")
    @Column(ignore = true)
    @Schema(description = "所属帖子")
    private Posts post;

    @Schema(description = "点赞数", example = "5")
    private Integer likeCount;

    @Schema(description = "是否删除，0-否，1-是", example = "0")
    private Integer isDeleted;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;

}
