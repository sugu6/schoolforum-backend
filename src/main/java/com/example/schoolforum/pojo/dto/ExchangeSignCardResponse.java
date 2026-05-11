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
@Schema(description = "兑换补签卡响应")
public class ExchangeSignCardResponse {

    @Schema(description = "是否兑换成功", example = "true")
    private Boolean success;

    @Schema(description = "提示消息", example = "兑换成功")
    private String message;

    @Schema(description = "消耗积分", example = "50")
    private Integer costPoints;

    @Schema(description = "剩余积分", example = "50")
    private Integer remainingPoints;

    @Schema(description = "当前补签卡数量", example = "3")
    private Integer signCards;
}
