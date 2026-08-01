package com.example.schoolforum.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import cn.dev33.satoken.annotation.SaMode;
import com.example.schoolforum.exception.BusinessException;
import com.example.schoolforum.pojo.dto.CombinedSearchResult;
import com.example.schoolforum.pojo.dto.KeywordSuggestion;
import com.example.schoolforum.service.SearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/search")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "搜索管理", description = "基于Manticore Search的全文搜索接口")
public class SearchController {

    private final SearchService searchService;
    private final Object syncLock = new Object();

    @GetMapping
    @Operation(summary = "综合搜索", description = "一次获取帖子和用户的搜索结果")
    public CombinedSearchResult search(
            @Parameter(description = "搜索关键词", required = true) @RequestParam String query,
            @Parameter(description = "页码，默认第1页") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页数量，默认10条") @RequestParam(defaultValue = "10") int size) {
        if (size > 100) size = 100;
        if (query.length() > 200) query = query.substring(0, 200);
        return searchService.search(query, page, size);
    }

    @GetMapping("/suggest")
    @Operation(summary = "搜索联想", description = "输入时实时获取关键词推荐")
    public List<KeywordSuggestion> suggest(
            @Parameter(description = "输入前缀", required = true) @RequestParam String prefix,
            @Parameter(description = "返回数量，默认8条") @RequestParam(defaultValue = "8") int limit) {
        if (prefix.length() > 200) prefix = prefix.substring(0, 200);
        if (limit < 1) limit = 8;
        if (limit > 20) limit = 20;
        return searchService.getKeywordSuggestions(prefix, limit);
    }

    @PostMapping("/sync")
    @Operation(summary = "重建索引", description = "删除旧索引并全量同步数据")
    @SaCheckRole(value = {"admin", "super_admin"}, mode = SaMode.OR)
    public String sync() {
        synchronized (syncLock) {
            try {
                searchService.deleteAllIndexes();
                searchService.syncAllPosts();
                searchService.syncAllUsers();
                long postCount = searchService.getPostsCollectionCount();
                long userCount = searchService.getUsersCollectionCount();
                // 强制落盘，避免容器重启后 RT 表回滚到旧快照
                searchService.flushAllIndexes();
                return String.format("索引重建完成（帖子：%d 条，用户：%d 条）", postCount, userCount);
            } catch (Exception e) {
                log.error("重建索引失败", e);
                throw new BusinessException("搜索服务连接失败，索引重建未完成，请稍后重试");
            }
        }
    }

    @DeleteMapping("/sync")
    @Operation(summary = "清空索引", description = "清空所有搜索索引数据")
    @SaCheckRole(value = {"admin", "super_admin"}, mode = SaMode.OR)
    public String clear() {
        synchronized (syncLock) {
            try {
                searchService.deleteAllIndexes();
                return "所有索引已清空";
            } catch (Exception e) {
                log.error("清空索引失败", e);
                throw new BusinessException("搜索服务连接失败，请稍后重试");
            }
        }
    }
}
