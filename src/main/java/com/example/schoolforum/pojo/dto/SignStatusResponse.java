package com.example.schoolforum.pojo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "签到状态响应")
public class SignStatusResponse {

    @Schema(description = "今日是否已签到", example = "false")
    private Boolean todaySigned;

    @Schema(description = "连续签到天数", example = "5")
    private Integer continuousDays;

    @Schema(description = "当前等级", example = "2")
    private Integer level;

    @Schema(description = "当前经验值", example = "150")
    private Integer exp;

    @Schema(description = "当前积分", example = "100")
    private Integer points;

    @Schema(description = "补签卡数量", example = "2")
    private Integer signCards;

    @Schema(description = "距离下一等级所需经验", example = "50")
    private Integer expToNextLevel;

    @Schema(description = "最后签到日期", example = "2024-01-12")
    private LocalDate lastSignDate;

    @Schema(description = "本月签到天数", example = "10")
    private Integer monthSignDays;
}
