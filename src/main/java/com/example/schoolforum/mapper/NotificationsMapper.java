package com.example.schoolforum.mapper;

import org.apache.ibatis.annotations.Mapper;
import com.mybatisflex.core.BaseMapper;
import com.example.schoolforum.pojo.Notifications;

/**
 * 系统通知表 映射层。
 *
 * @author sugu
 * @since 2026-03-06
 */
@Mapper
public interface NotificationsMapper extends BaseMapper<Notifications> {

}
