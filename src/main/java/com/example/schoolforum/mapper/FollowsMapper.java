package com.example.schoolforum.mapper;

import org.apache.ibatis.annotations.Mapper;
import com.mybatisflex.core.BaseMapper;
import com.example.schoolforum.pojo.Follows;

/**
 * 关注表 映射层。
 *
 * @author sugu
 * @since 2026-03-07
 */
@Mapper
public interface FollowsMapper extends BaseMapper<Follows> {

}
