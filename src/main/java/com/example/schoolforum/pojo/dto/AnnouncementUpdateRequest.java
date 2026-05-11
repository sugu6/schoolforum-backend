package com.example.schoolforum.pojo.dto;

import com.example.schoolforum.enums.AnnouncementType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "更新公告请求")
public class AnnouncementUpdateRequest {

    @Size(max = 200, message = "公告标题不能超过200个字符")
    @Schema(description = "公告标题", example = "系统维护通知（更新）")
    private String title;

    @Schema(description = "公告内容（Markdown格式）")
    private String content;

    @Schema(description = "公告类型：INFO-普通通知、IMPORTANT-重要公告、URGENT-紧急公告", example = "IMPORTANT")
    private AnnouncementType type;
}
