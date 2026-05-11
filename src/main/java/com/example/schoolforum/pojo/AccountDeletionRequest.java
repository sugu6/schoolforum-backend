package com.example.schoolforum.pojo;

import com.example.schoolforum.enums.DeletionStatus;
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
@Table("account_deletion_requests")
@Schema(description = "账户注销申请实体")
public class AccountDeletionRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id(keyType = KeyType.Auto)
    @Schema(description = "申请ID", example = "1")
    private Long id;

    @Schema(description = "用户ID", example = "1")
    private Long userId;

    @Schema(description = "注销原因", example = "不再使用此账号")
    private String reason;

    @Schema(description = "状态", example = "PENDING")
    private DeletionStatus status;

    @Schema(description = "申请时间")
    private LocalDateTime requestedAt;

    @Schema(description = "计划执行时间（冷静期结束）")
    private LocalDateTime scheduledAt;

    @Schema(description = "实际完成时间")
    private LocalDateTime completedAt;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;
}