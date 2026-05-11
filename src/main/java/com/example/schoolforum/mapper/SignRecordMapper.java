package com.example.schoolforum.mapper;

import com.example.schoolforum.pojo.SignRecord;
import com.mybatisflex.core.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 签到记录表 映射层。
 *
 * @author sugu
 * @since 2026-04-13
 */
@Mapper
public interface SignRecordMapper extends BaseMapper<SignRecord> {

}
