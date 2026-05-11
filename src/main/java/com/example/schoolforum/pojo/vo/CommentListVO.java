package com.example.schoolforum.pojo.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "评论列表响应")
public class CommentListVO {

    @Schema(description = "评论总数（包括已删除）")
    private Long total;

    @Schema(description = "评论列表")
    private List<CommentVO> list;

    public static CommentListVO of(Long total, List<CommentVO> list) {
        CommentListVO vo = new CommentListVO();
        vo.setTotal(total);
        vo.setList(list);
        return vo;
    }
}
