package com.example.schoolforum.pojo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "签到响应")
public class SignResponse {

    @Schema(description = "是否签到成功", example = "true")
    private Boolean success;

    @Schema(description = "提示消息", example = "签到成功")
    private String message;

    @Schema(description = "获得经验值", example = "15")
    private Integer expGained;

    @Schema(description = "获得积分", example = "10")
    private Integer pointsGained;

    @Schema(description = "当前经验值", example = "150")
    private Integer currentExp;

    @Schema(description = "当前等级", example = "2")
    private Integer currentLevel;

    @Schema(description = "当前积分", example = "100")
    private Integer currentPoints;

    @Schema(description = "连续签到天数", example = "7")
    private Integer continuousDays;

    @Schema(description = "是否升级", example = "false")
    private Boolean levelUp;

    @Schema(description = "新等级（升级时返回）", example = "3")
    private Integer newLevel;
}
