package com.example.schoolforum.pojo.dto;

import com.example.schoolforum.pojo.Posts;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.format.DateTimeFormatter;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "帖子搜索文档")
@JsonPropertyOrder({
        "id", "authorId", "authorName", "authorAvatar",
        "title", "content", "coverImage",
        "categoryId", "categoryName", "parentCategoryName", "tagNames",
        "likeCount", "commentCount", "favoriteCount", "viewCount",
        "isPinned", "isEssential",
        "createdAt", "updatedAt"
})
public class PostSearchDocument {

    @Schema(description = "帖子ID")
    private Long id;

    @Schema(description = "作者ID")
    private Long authorId;

    @Schema(description = "作者名称")
    private String authorName;

    @Schema(description = "作者头像URL")
    private String authorAvatar;

    @Schema(description = "帖子标题")
    private String title;

    @Schema(description = "帖子内容")
    private String content;

    @Schema(description = "分类ID")
    private Long categoryId;

    @Schema(description = "分类名称")
    private String categoryName;

    @Schema(description = "一级分类名称")
    private String parentCategoryName;

    @Schema(description = "标签名称列表")
    private List<String> tagNames;

    @Schema(description = "封面图片URL")
    private String coverImage;

    @Schema(description = "点赞数")
    private Integer likeCount;

    @Schema(description = "评论数")
    private Integer commentCount;

    @Schema(description = "收藏数")
    private Integer favoriteCount;

    @Schema(description = "浏览数")
    private Integer viewCount;

    @Schema(description = "是否置顶")
    private String isPinned;

    @Schema(description = "是否精华")
    private String isEssential;

    @Schema(description = "创建时间")
    private String createdAt;

    @Schema(description = "更新时间")
    private String updatedAt;

    public static PostSearchDocument fromEntity(Posts post) {
        return PostSearchDocument.builder()
                .id(post.getId())
                .authorId(post.getAuthorId())
                .authorName(post.getAuthorName())
                .authorAvatar(post.getAuthorAvatar())
                .title(post.getTitle())
                .content(post.getContent())
                .categoryId(post.getCategoryId())
                .categoryName(post.getCategoryName())
                .parentCategoryName(post.getParentCategoryName())
                .tagNames(post.getTagNames())
                .coverImage(post.getCoverImage())
                .likeCount(post.getLikeCount())
                .commentCount(post.getCommentCount())
                .favoriteCount(post.getFavoriteCount())
                .viewCount(post.getViewCount())
                .isPinned(post.getIsPinned() != null ? post.getIsPinned().name() : "NOT_PINNED")
                .isEssential(post.getIsEssential() != null ? post.getIsEssential().name() : "NOT_ESSENTIAL")
                .createdAt(post.getCreatedAt() != null ? post.getCreatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : null)
                .updatedAt(post.getUpdatedAt() != null ? post.getUpdatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : null)
                .build();
    }
}
