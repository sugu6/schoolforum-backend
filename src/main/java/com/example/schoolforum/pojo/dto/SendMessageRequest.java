package com.example.schoolforum.pojo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "发送私信请求")
public class SendMessageRequest {

    @NotNull(message = "接收者ID不能为空")
    @Schema(description = "接收者ID", example = "2")
    private Long receiverId;

    @NotNull(message = "消息内容不能为空")
    @Size(min = 1, max = 500, message = "消息内容长度必须在1-500个字符之间")
    @Schema(description = "消息内容", example = "你好，在吗？")
    private String content;

}
