package com.example.schoolforum.mapper;

import org.apache.ibatis.annotations.Mapper;
import com.mybatisflex.core.BaseMapper;
import com.example.schoolforum.pojo.PrivateMessage;

/**
 * 私信消息表 映射层。
 *
 * @author sugu
 * @since 2026-03-07
 */
@Mapper
public interface PrivateMessageMapper extends BaseMapper<PrivateMessage> {

}
