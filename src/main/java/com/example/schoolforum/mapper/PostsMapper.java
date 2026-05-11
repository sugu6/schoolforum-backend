package com.example.schoolforum.mapper;

import org.apache.ibatis.annotations.Mapper;
import com.mybatisflex.core.BaseMapper;
import com.example.schoolforum.pojo.Posts;

/**
 * 帖子表 映射层。
 *
 * @author sugu
 * @since 2026-02-17
 */
@Mapper
public interface PostsMapper extends BaseMapper<Posts> {

}
