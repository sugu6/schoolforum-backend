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
@Schema(description = "搜索结果")
public class SearchResult<T> {

    @Schema(description = "搜索关键词")
    private String query;

    @Schema(description = "处理时间(毫秒)")
    private Long processingTimeMs;

    @Schema(description = "总结果数")
    private Long totalHits;

    @Schema(description = "当前页结果数")
    private Integer hitsPerPage;

    @Schema(description = "当前页码(从1开始)")
    private Integer page;

    @Schema(description = "总页数")
    private Integer totalPages;

    @Schema(description = "搜索结果列表")
    private List<T> hits;
}
