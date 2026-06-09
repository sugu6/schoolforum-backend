package com.example.schoolforum.config;

import com.example.schoolforum.websocket.PostStatsWebSocketHandler;
import com.example.schoolforum.websocket.PrivateMessageWebSocketHandler;
import com.example.schoolforum.websocket.WebSocketAuthInterceptor;
import jakarta.servlet.Filter;
import lombok.RequiredArgsConstructor;
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

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(privateMessageWebSocketHandler, "/ws/message")
                .addInterceptors(webSocketAuthInterceptor)
                .setAllowedOrigins("https://schoolforum.sugu6.top", "http://localhost:5173", "http://localhost:3000", "http://localhost:8080");

        registry.addHandler(postStatsWebSocketHandler, "/ws/post-stats")
                .addInterceptors(webSocketAuthInterceptor)
                .setAllowedOrigins("https://schoolforum.sugu6.top", "http://localhost:5173", "http://localhost:3000", "http://localhost:8080");
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
