package com.example.schoolforum.pojo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "关键词推荐结果")
public class KeywordSuggestion {

    @Schema(description = "关键词")
    private String keyword;

    @Schema(description = "搜索次数")
    private Long count;

    @Schema(description = "相关度分数")
    private Double score;
}
