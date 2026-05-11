package com.example.schoolforum.mapper;

import org.apache.ibatis.annotations.Mapper;
import com.mybatisflex.core.BaseMapper;
import com.example.schoolforum.pojo.BrowseHistory;

/**
 * 浏览历史表 映射层。
 *
 * @author sugu
 * @since 2026-05-10
 */
@Mapper
public interface BrowseHistoryMapper extends BaseMapper<BrowseHistory> {

}
