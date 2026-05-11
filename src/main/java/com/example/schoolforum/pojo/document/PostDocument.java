package com.example.schoolforum.pojo.document;

import com.example.schoolforum.pojo.Posts;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.*;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(indexName = "schoolforum_posts", createIndex = false)
public class PostDocument {

    @Id
    private Long id;

    @Field(type = FieldType.Long, name = "author_id")
    private Long authorId;

    @Field(type = FieldType.Text, name = "author_name", analyzer = "ik_max_word", searchAnalyzer = "ik_smart")
    private String authorName;

    @Field(type = FieldType.Keyword, name = "author_avatar")
    private String authorAvatar;

    @Field(type = FieldType.Text, analyzer = "ik_max_word", searchAnalyzer = "ik_smart")
    private String title;

    @Field(type = FieldType.Text, analyzer = "ik_max_word", searchAnalyzer = "ik_smart")
    private String content;

    @Field(type = FieldType.Long, name = "category_id")
    private Long categoryId;

    @Field(type = FieldType.Keyword, name = "category_name")
    private String categoryName;

    @Field(type = FieldType.Keyword, name = "parent_category_name")
    private String parentCategoryName;

    @Field(type = FieldType.Keyword)
    private List<String> tags;

    @Field(type = FieldType.Integer, name = "like_count")
    private Integer likeCount;

    @Field(type = FieldType.Integer, name = "view_count")
    private Integer viewCount;

    @Field(type = FieldType.Integer, name = "comment_count")
    private Integer commentCount;

    @Field(type = FieldType.Integer, name = "favorite_count")
    private Integer favoriteCount;

    @Field(type = FieldType.Keyword, name = "cover_image")
    private String coverImage;

    @Field(type = FieldType.Boolean, name = "is_pinned")
    private Boolean isPinned;

    @Field(type = FieldType.Boolean, name = "is_essential")
    private Boolean isEssential;

    @Field(type = FieldType.Long, name = "created_at")
    private Long createdAt;

    @Field(type = FieldType.Long, name = "updated_at")
    private Long updatedAt;

    public static PostDocument fromEntity(Posts post) {
        return PostDocument.builder()
                .id(post.getId())
                .authorId(post.getAuthorId())
                .authorName(post.getAuthorName())
                .authorAvatar(post.getAuthorAvatar())
                .title(post.getTitle())
                .content(post.getContent())
                .categoryId(post.getCategoryId())
                .categoryName(post.getCategoryName())
                .parentCategoryName(post.getParentCategoryName())
                .tags(post.getTagNames())
                .coverImage(post.getCoverImage())
                .likeCount(post.getLikeCount())
                .viewCount(post.getViewCount())
                .commentCount(post.getCommentCount())
                .favoriteCount(post.getFavoriteCount())
                .isPinned(post.getIsPinned() != null && post.getIsPinned().name().equals("PINNED"))
                .isEssential(post.getIsEssential() != null && post.getIsEssential().name().equals("ESSENTIAL"))
                .createdAt(toTimestamp(post.getCreatedAt()))
                .updatedAt(toTimestamp(post.getUpdatedAt()))
                .build();
    }

    private static Long toTimestamp(LocalDateTime dateTime) {
        if (dateTime == null) return null;
        return dateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }
}
