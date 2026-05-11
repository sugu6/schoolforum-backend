package com.example.schoolforum.mapper;

import org.apache.ibatis.annotations.Mapper;
import com.mybatisflex.core.BaseMapper;
import com.mybatisflex.core.query.QueryWrapper;
import com.example.schoolforum.pojo.Comments;

/**
 * 评论表 映射层。
 *
 * @author sugu
 * @since 2026-02-17
 */
@Mapper
public interface CommentsMapper extends BaseMapper<Comments> {

    default int countByPostId(Long postId) {
        return Math.toIntExact(selectCountByQuery(
            QueryWrapper.create().where(Comments::getPostId).eq(postId)
                         .and(Comments::getIsDeleted).eq(0)
        ));
    }
}
