package com.example.schoolforum.mapper;

import org.apache.ibatis.annotations.Mapper;
import com.mybatisflex.core.BaseMapper;
import com.example.schoolforum.pojo.Tags;

/**
 * 标签表 映射层。
 *
 * @author sugu
 * @since 2026-03-08
 */
@Mapper
public interface TagsMapper extends BaseMapper<Tags> {

}
