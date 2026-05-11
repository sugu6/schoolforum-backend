package com.example.schoolforum.service;

import com.example.schoolforum.pojo.dto.*;
import com.example.schoolforum.pojo.SignRecord;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.service.IService;

import java.time.LocalDate;
import java.util.List;

/**
 * 签到服务层。
 *
 * @author sugu
 * @since 2026-04-13
 */
public interface SignService extends IService<SignRecord> {

    /**
     * 每日签到
     */
    SignResponse sign(Long userId);

    /**
     * 获取签到状态
     */
    SignStatusResponse getSignStatus(Long userId);

    /**
     * 补签
     */
    RepairSignResponse repairSign(Long userId, LocalDate signDate);

    /**
     * 兑换补签卡
     */
    ExchangeSignCardResponse exchangeSignCard(Long userId);

    /**
     * 获取签到记录
     */
    Page<SignRecordVO> getSignRecords(Long userId, int pageNumber, int pageSize);

    /**
     * 获取某月签到日历
     */
    List<LocalDate> getSignCalendar(Long userId, int year, int month);

    /**
     * 增加积分（供其他模块调用）
     */
    void addPoints(Long userId, Integer points, String type, Long relatedId, String description);

    /**
     * 扣减积分（供其他模块调用）
     */
    void deductPoints(Long userId, Integer points, String type, Long relatedId, String description);
}
