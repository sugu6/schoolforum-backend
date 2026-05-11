package com.example.schoolforum.mapper;

import org.apache.ibatis.annotations.Mapper;
import com.mybatisflex.core.BaseMapper;
import com.example.schoolforum.pojo.Conversation;

/**
 * 私信会话表 映射层。
 *
 * @author sugu
 * @since 2026-03-07
 */
@Mapper
public interface ConversationMapper extends BaseMapper<Conversation> {

}
