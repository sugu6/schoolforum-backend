package com.example.schoolforum.pojo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "补签响应")
public class RepairSignResponse {

    @Schema(description = "是否补签成功", example = "true")
    private Boolean success;

    @Schema(description = "提示消息", example = "补签成功")
    private String message;

    @Schema(description = "获得经验值", example = "5")
    private Integer expGained;

    @Schema(description = "获得积分", example = "2")
    private Integer pointsGained;

    @Schema(description = "剩余补签卡数量", example = "1")
    private Integer remainingCards;
}
