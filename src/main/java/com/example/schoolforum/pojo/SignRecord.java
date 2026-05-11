package com.example.schoolforum.pojo;

import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("sign_records")
@Schema(description = "签到记录")
public class SignRecord implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id(keyType = KeyType.Auto)
    @Schema(description = "记录ID", example = "1")
    private Long id;

    @Schema(description = "用户ID", example = "1")
    private Long userId;

    @Schema(description = "签到日期", example = "2024-01-01")
    private LocalDate signDate;

    @Schema(description = "获得经验值", example = "10")
    private Integer expGained;

    @Schema(description = "获得积分", example = "5")
    private Integer pointsGained;

    @Schema(description = "签到时连续天数", example = "7")
    private Integer continuousDays;

    @Schema(description = "是否补签", example = "false")
    private Boolean isRepair;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;
}
