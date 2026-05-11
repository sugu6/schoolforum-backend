package com.example.schoolforum.pojo;

import com.example.schoolforum.enums.PointsType;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("points_records")
@Schema(description = "积分流水记录")
public class PointsRecord implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id(keyType = KeyType.Auto)
    @Schema(description = "记录ID", example = "1")
    private Long id;

    @Schema(description = "用户ID", example = "1")
    private Long userId;

    @Schema(description = "变动数量(正为获得,负为消费)", example = "10")
    private Integer changeAmount;

    @Schema(description = "变动后余额", example = "100")
    private Integer balanceAfter;

    @Schema(description = "类型", example = "SIGN")
    private PointsType type;

    @Schema(description = "关联ID", example = "1")
    private Long relatedId;

    @Schema(description = "描述", example = "每日签到")
    private String description;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;
}
