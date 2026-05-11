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
@Schema(description = "签到记录VO")
public class SignRecordVO {

    @Schema(description = "记录ID", example = "1")
    private Long id;

    @Schema(description = "签到日期", example = "2024-01-01")
    private LocalDate signDate;

    @Schema(description = "获得经验值", example = "15")
    private Integer expGained;

    @Schema(description = "获得积分", example = "10")
    private Integer pointsGained;

    @Schema(description = "签到时连续天数", example = "7")
    private Integer continuousDays;

    @Schema(description = "是否补签", example = "false")
    private Boolean isRepair;
}
