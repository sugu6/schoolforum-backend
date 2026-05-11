package com.example.schoolforum.pojo;

import com.example.schoolforum.enums.EssentialStatus;
import com.example.schoolforum.enums.PinnedStatus;
import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serializable;
import java.time.LocalDateTime;

import java.io.Serial;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("posts")
@Schema(description = "帖子实体")
public class Posts implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id(keyType = KeyType.Auto)
    @Schema(description = "帖子ID", example = "1")
    private Long id;

    @Schema(description = "作者ID", example = "1")
    private Long authorId;

    @Column(ignore = true)
    @Schema(description = "作者名")
    private String authorName;

    @Column(ignore = true)
    @Schema(description = "作者头像URL")
    private String authorAvatar;

    @Schema(description = "帖子标题", example = "如何学习Java")
    private String title;

    @Schema(description = "帖子内容（Markdown格式）", example = "# 标题\n\n这是帖子内容...")
    private String content;

    @Schema(description = "分类ID", example = "1")
    private Long categoryId;

    @Column(ignore = true)
    @Schema(description = "分类名称（二级分类）")
    private String categoryName;

    @Column(ignore = true)
    @Schema(description = "一级分类名称")
    private String parentCategoryName;

    @Column(ignore = true)
    @Schema(description = "标签名称列表")
    private List<String> tagNames;

    @Schema(description = "封面图片URL", example = "https://picsum.photos/800/400?random=1")
    private String coverImage;

    @Schema(description = "点赞数", example = "10")
    private Integer likeCount;

    @Schema(description = "评论数", example = "5")
    private Integer commentCount;

    @Schema(description = "收藏数", example = "8")
    private Integer favoriteCount;

    @Schema(description = "浏览数", example = "100")
    private Integer viewCount;

    @Schema(description = "是否置顶")
    private PinnedStatus isPinned;

    @Schema(description = "是否精华")
    private EssentialStatus isEssential;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;
}
