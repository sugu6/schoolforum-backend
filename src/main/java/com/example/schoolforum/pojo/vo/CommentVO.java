package com.example.schoolforum.pojo.vo;

import com.example.schoolforum.pojo.Comments;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Schema(description = "评论响应VO")
public class CommentVO {

    @Schema(description = "评论ID")
    private Long id;

    @Schema(description = "评论作者ID")
    private Long authorId;

    @Schema(description = "评论作者名称")
    private String authorName;

    @Schema(description = "评论作者头像")
    private String authorAvatar;

    @Schema(description = "评论内容")
    private String content;

    @Schema(description = "父评论ID")
    private Long parentId;

    @Schema(description = "点赞数")
    private Integer likeCount;

    @Schema(description = "是否已删除")
    private Boolean deleted;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    @Schema(description = "子评论列表")
    private List<CommentVO> replies;

    public static CommentVO from(Comments comment, String authorName, String authorAvatar) {
        CommentVO vo = new CommentVO();
        vo.setId(comment.getId());
        vo.setAuthorId(comment.getAuthorId());
        vo.setAuthorName(authorName);
        vo.setAuthorAvatar(authorAvatar);
        vo.setContent(comment.getContent());
        vo.setParentId(comment.getParentId());
        vo.setLikeCount(comment.getLikeCount());
        vo.setDeleted(comment.getIsDeleted() == 1);
        vo.setCreatedAt(comment.getCreatedAt());
        return vo;
    }
}
