package com.example.schoolforum.config;

import com.example.schoolforum.websocket.PostStatsWebSocketHandler;
import com.example.schoolforum.websocket.PrivateMessageWebSocketHandler;
import com.example.schoolforum.websocket.WebSocketAuthInterceptor;
import jakarta.servlet.Filter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * WebSocket 配置类
 *
 * @author sugu
 * @since 2026-03-07
 */
@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketConfigurer {

    private final PrivateMessageWebSocketHandler privateMessageWebSocketHandler;
    private final PostStatsWebSocketHandler postStatsWebSocketHandler;
    private final WebSocketAuthInterceptor webSocketAuthInterceptor;

    @Value("${cors.allowed-origins:}")
    private String corsAllowedOrigins;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        String[] origins = corsAllowedOrigins.isEmpty()
                ? new String[]{"http://localhost:5173", "http://localhost:8080"}
                : corsAllowedOrigins.split("\\s*,\\s*");

        // WebSocket 端点使用 /ws/ 前缀
        // token 通过首条 auth 消息发送，URL 不携带 token
        registry.addHandler(privateMessageWebSocketHandler, "/ws/message")
                .addInterceptors(webSocketAuthInterceptor)
                .setAllowedOrigins(origins);

        registry.addHandler(postStatsWebSocketHandler, "/ws/post-stats")
                .addInterceptors(webSocketAuthInterceptor)
                .setAllowedOrigins(origins);
    }

    /**
     * 覆盖 Spring Boot 自动配置的 tomcatWsFilter
     * Tomcat 的 WsFilter 与 Spring @EnableWebSocket 冲突，导致 WebSocket 请求被拦截返回 400
     */
    @Bean("tomcatWsFilter")
    public FilterRegistrationBean<Filter> tomcatWsFilterOverride() {
        FilterRegistrationBean<Filter> registration = new FilterRegistrationBean<>();
        registration.setFilter((Filter) (request, response, chain) -> chain.doFilter(request, response));
        registration.addUrlPatterns("/*");
        registration.setName("tomcatWsFilter");
        return registration;
    }
}
