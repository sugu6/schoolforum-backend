package com.example.schoolforum.pojo.dto;

import com.example.schoolforum.enums.AnnouncementType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "创建公告请求")
public class AnnouncementCreateRequest {

    @NotBlank(message = "公告标题不能为空")
    @Size(max = 200, message = "公告标题不能超过200个字符")
    @Schema(description = "公告标题", example = "系统维护通知", requiredMode = Schema.RequiredMode.REQUIRED)
    private String title;

    @NotBlank(message = "公告内容不能为空")
    @Schema(description = "公告内容（Markdown格式）", example = "# 维护通知\n\n系统将于今晚进行维护...", requiredMode = Schema.RequiredMode.REQUIRED)
    private String content;

    @NotNull(message = "公告类型不能为空")
    @Schema(description = "公告类型：INFO-普通通知、IMPORTANT-重要公告、URGENT-紧急公告", example = "INFO", requiredMode = Schema.RequiredMode.REQUIRED)
    private AnnouncementType type;

    @Schema(description = "是否立即发布，默认为false（保存为草稿）", example = "false")
    private Boolean publish;
}
