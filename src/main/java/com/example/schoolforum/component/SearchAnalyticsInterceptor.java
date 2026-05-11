package com.example.schoolforum.component;

import com.example.schoolforum.pojo.document.PopularQueryDocument;
import com.example.schoolforum.repository.PopularQueryRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class SearchAnalyticsInterceptor implements HandlerInterceptor {

    private final PopularQueryRepository popularQueryRepository;

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        String requestURI = request.getRequestURI();

        if ((requestURI.equals("/search") || requestURI.startsWith("/search/")) && !requestURI.contains("/sync") && !requestURI.contains("/index") && !requestURI.contains("/health")) {
            String query = request.getParameter("query");
            if (query != null && !query.trim().isEmpty()) {
                recordOrIncrement(query.trim());
            }
        }
    }

    private void recordOrIncrement(String keyword) {
        try {
            Optional<PopularQueryDocument> existing = popularQueryRepository.findById(keyword);
            if (existing.isPresent()) {
                PopularQueryDocument doc = existing.get();
                doc.setCount(doc.getCount() + 1);
                popularQueryRepository.save(doc);
            } else {
                PopularQueryDocument doc = PopularQueryDocument.builder()
                        .id(keyword)
                        .keyword(keyword)
                        .count(1L)
                        .build();
                popularQueryRepository.save(doc);
            }
        } catch (Exception e) {
            log.debug("Failed to record search query '{}': {}", keyword, e.getMessage());
        }
    }
}