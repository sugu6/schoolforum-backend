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
@Schema(description = "综合搜索结果")
public class CombinedSearchResult {

    @Schema(description = "帖子搜索结果")
    private SearchResult<PostSearchDocument> posts;

    @Schema(description = "用户搜索结果")
    private SearchResult<UserSearchDocument> users;
}
