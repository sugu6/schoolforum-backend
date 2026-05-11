package com.example.schoolforum.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.stp.StpUtil;
import com.example.schoolforum.pojo.dto.*;
import com.example.schoolforum.service.SignService;
import com.mybatisflex.core.paginate.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/sign")
@RequiredArgsConstructor
@Tag(name = "签到系统", description = "签到、补签、积分兑换等相关接口")
public class SignController {

    private final SignService signService;

    @PostMapping("/daily")
    @SaCheckLogin
    @Operation(summary = "每日签到", description = "用户每日签到，获得经验值和积分")
    public SignResponse dailySign() {
        Long userId = StpUtil.getLoginIdAsLong();
        return signService.sign(userId);
    }

    @GetMapping("/status")
    @SaCheckLogin
    @Operation(summary = "获取签到状态", description = "获取当前用户的签到状态、等级、积分等信息")
    public SignStatusResponse getSignStatus() {
        Long userId = StpUtil.getLoginIdAsLong();
        return signService.getSignStatus(userId);
    }

    @PostMapping("/repair")
    @SaCheckLogin
    @Operation(summary = "补签", description = "使用补签卡补签近2个月内的日期")
    public RepairSignResponse repairSign(
            @Parameter(description = "补签日期，格式：yyyy-MM-dd（如：2026-04-29）")
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate signDate) {
        Long userId = StpUtil.getLoginIdAsLong();
        return signService.repairSign(userId, signDate);
    }

    @PostMapping("/exchange-card")
    @SaCheckLogin
    @Operation(summary = "兑换补签卡", description = "使用50积分兑换1张补签卡")
    public ExchangeSignCardResponse exchangeSignCard() {
        Long userId = StpUtil.getLoginIdAsLong();
        return signService.exchangeSignCard(userId);
    }

    @GetMapping("/records")
    @SaCheckLogin
    @Operation(summary = "获取签到记录", description = "分页获取当前用户的签到记录")
    public Page<SignRecordVO> getSignRecords(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int pageNumber,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "10") int pageSize) {
        Long userId = StpUtil.getLoginIdAsLong();
        return signService.getSignRecords(userId, pageNumber, pageSize);
    }

    @GetMapping("/calendar")
    @SaCheckLogin
    @Operation(summary = "获取签到日历", description = "获取某月的签到日期列表")
    public List<LocalDate> getSignCalendar(
            @Parameter(description = "年份") @RequestParam int year,
            @Parameter(description = "月份") @RequestParam int month) {
        Long userId = StpUtil.getLoginIdAsLong();
        return signService.getSignCalendar(userId, year, month);
    }
}
