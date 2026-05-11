package com.example.schoolforum.mapper;

import org.apache.ibatis.annotations.Mapper;
import com.mybatisflex.core.BaseMapper;
import com.example.schoolforum.pojo.PostTags;

/**
 * 帖子标签关联表 映射层。
 *
 * @author sugu
 * @since 2026-03-09
 */
@Mapper
public interface PostTagsMapper extends BaseMapper<PostTags> {

}
