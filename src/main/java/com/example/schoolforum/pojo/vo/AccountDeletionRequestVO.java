package com.example.schoolforum.pojo.vo;

import com.example.schoolforum.enums.DeletionStatus;
import com.example.schoolforum.pojo.AccountDeletionRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "账户注销申请响应VO")
public class AccountDeletionRequestVO {

    @Schema(description = "申请ID", example = "1")
    private Long id;

    @Schema(description = "用户ID", example = "1")
    private Long userId;

    @Schema(description = "用户名", example = "张三")
    private String username;

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

    public static AccountDeletionRequestVO from(AccountDeletionRequest request, String username) {
        AccountDeletionRequestVO vo = new AccountDeletionRequestVO();
        vo.setId(request.getId());
        vo.setUserId(request.getUserId());
        vo.setUsername(username);
        vo.setReason(request.getReason());
        vo.setStatus(request.getStatus());
        vo.setRequestedAt(request.getRequestedAt());
        vo.setScheduledAt(request.getScheduledAt());
        vo.setCompletedAt(request.getCompletedAt());
        vo.setCreatedAt(request.getCreatedAt());
        return vo;
    }
}
