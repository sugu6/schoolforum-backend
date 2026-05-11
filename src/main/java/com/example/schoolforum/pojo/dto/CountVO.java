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
@Schema(description = "统计数据响应")
public class CountVO {

    @Schema(description = "用户总数", example = "100")
    private Long userCount;

    @Schema(description = "评论总数", example = "500")
    private Long commentCount;

    @Schema(description = "帖子总数", example = "200")
    private Long postCount;

    @Schema(description = "公告总数", example = "10")
    private Long announcementCount;

    @Schema(description = "分类总数", example = "5")
    private Long categoryCount;

    @Schema(description = "标签总数", example = "20")
    private Long tagCount;
}
