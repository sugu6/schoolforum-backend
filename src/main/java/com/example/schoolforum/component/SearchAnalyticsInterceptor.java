package com.example.schoolforum.component;

import com.example.schoolforum.pojo.document.PopularQueryDocument;
import com.manticoresearch.client.api.IndexApi;
import com.manticoresearch.client.api.SearchApi;
import com.manticoresearch.client.api.UtilsApi;
import com.manticoresearch.client.model.InsertDocumentRequest;
import com.manticoresearch.client.model.SearchRequest;
import com.manticoresearch.client.model.SearchResponse;
import com.manticoresearch.client.model.UpdateDocumentRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Component
@RequiredArgsConstructor
public class SearchAnalyticsInterceptor implements HandlerInterceptor {

    /**
     * 使用 IF NOT EXISTS 幂等建表，避免依赖"索引不存在"的特定错误码（Manticore 对未知表返回 500 而非 404）。
     * 联想词功能不依赖此表；本表仅用于热词统计。
     */
    private static final String POPULAR_QUERIES_DDL =
            "CREATE TABLE IF NOT EXISTS " + PopularQueryDocument.INDEX_NAME + " ("
                    + "keyword TEXT, "
                    + "count BIGINT"
                    + ") charset_table = '0..9, A..Z->a..z, _, a..z, chinese' morphology = 'icu_chinese' min_prefix_len = '1'";

    private final IndexApi indexApi;
    private final SearchApi searchApi;
    private final UtilsApi utilsApi;

    private final AtomicBoolean indexEnsured = new AtomicBoolean(false);

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        String requestURI = request.getRequestURI();

        if ((requestURI.equals("/search") || requestURI.startsWith("/search/"))
                && !requestURI.contains("/sync") && !requestURI.contains("/index") && !requestURI.contains("/health")) {

            String query = request.getParameter("query");
            if (query != null && !query.trim().isEmpty()) {
                recordOrIncrement(query.trim());
            }

            String prefix = request.getParameter("prefix");
            if (prefix != null && !prefix.trim().isEmpty()) {
                recordOrIncrement(prefix.trim());
            }
        }
    }

    private void recordOrIncrement(String keyword) {
        try {
            ensurePopularQueriesIndex();

            SearchRequest searchRequest = new SearchRequest();
            searchRequest.setIndex(PopularQueryDocument.INDEX_NAME);
            Map<String, Object> matchQuery = new HashMap<>();
            matchQuery.put("keyword", keyword);
            Map<String, Object> queryMap = new HashMap<>();
            queryMap.put("match", matchQuery);
            searchRequest.setQuery(queryMap);
            searchRequest.setLimit(1);

            SearchResponse searchResponse = searchApi.search(searchRequest);
            List<?> rawHits = searchResponse.getHits().getHits();

            if (!rawHits.isEmpty()) {
                @SuppressWarnings("unchecked")
                Map<String, Object> hit = (Map<String, Object>) rawHits.get(0);
                Object idObj = hit.get("_id");
                Long docId = idObj instanceof Number ? ((Number) idObj).longValue() : Long.parseLong(idObj.toString());

                @SuppressWarnings("unchecked")
                Map<String, Object> source = hit.get("_source") instanceof Map
                        ? (Map<String, Object>) hit.get("_source")
                        : new HashMap<>();
                Object countObj = source.get("count");
                long currentCount = countObj instanceof Number ? ((Number) countObj).longValue() : 0L;

                UpdateDocumentRequest updateRequest = new UpdateDocumentRequest();
                Map<String, Object> doc = new HashMap<>();
                doc.put("count", currentCount + 1);
                updateRequest.index(PopularQueryDocument.INDEX_NAME).id(docId).setDoc(doc);
                indexApi.update(updateRequest);
            } else {
                InsertDocumentRequest insertRequest = new InsertDocumentRequest();
                Map<String, Object> doc = new HashMap<>();
                doc.put("keyword", keyword);
                doc.put("count", 1L);
                insertRequest.index(PopularQueryDocument.INDEX_NAME).setDoc(doc);
                indexApi.insert(insertRequest);
            }
        } catch (Exception e) {
            log.debug("Failed to record search query '{}': {}", keyword, e.getMessage());
        }
    }

    private void ensurePopularQueriesIndex() {
        if (indexEnsured.get()) {
            return;
        }
        try {
            utilsApi.sql(POPULAR_QUERIES_DDL, true);
            indexEnsured.set(true);
            log.info("Created popular_queries index with ICU Chinese morphology");
        } catch (Exception e) {
            log.debug("Failed to ensure popular queries index: {}", e.getMessage());
        }
    }
}
