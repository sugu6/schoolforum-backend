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
@Schema(description = "未读数量响应")
public class UnreadCountVO {

    @Schema(description = "未读数量", example = "5")
    private Long unreadCount;
}
