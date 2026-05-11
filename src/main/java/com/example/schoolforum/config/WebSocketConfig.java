package com.example.schoolforum.config;

import com.example.schoolforum.websocket.PostStatsWebSocketHandler;
import com.example.schoolforum.websocket.PrivateMessageWebSocketHandler;
import com.example.schoolforum.websocket.WebSocketAuthInterceptor;
import lombok.RequiredArgsConstructor;
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
                .setAllowedOrigins("*");

        registry.addHandler(postStatsWebSocketHandler, "/ws/post-stats")
                .setAllowedOrigins("*");
    }

}
