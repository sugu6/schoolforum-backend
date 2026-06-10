package com.example.schoolforum.websocket;

import cn.dev33.satoken.stp.StpUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

/**
 * WebSocket 握手拦截器
 * 允许不带 token 的握手，token 通过首条 auth 消息发送
 * 避免 URL 中携带 token 被广告拦截器拦截
 *
 * @author sugu
 * @since 2026-03-07
 */
@Slf4j
@Component
public class WebSocketAuthInterceptor implements HandshakeInterceptor {

    public static final String USER_ID_KEY = "userId";

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                    WebSocketHandler wsHandler, Map<String, Object> attributes) throws Exception {
        if (request instanceof ServletServerHttpRequest servletRequest) {
            // 不再从 URL 提取 token，允许所有握手通过
            // 认证通过首条 auth 消息完成
            log.debug("WebSocket 握手: 等待 auth 消息认证, uri={}", servletRequest.getURI().getPath());
            return true;
        }

        response.setStatusCode(org.springframework.http.HttpStatus.FORBIDDEN);
        return false;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                WebSocketHandler wsHandler, Exception exception) {
        if (exception != null) {
            log.error("WebSocket 握手异常: {}", exception.getMessage());
        }
    }

    /**
     * 验证 token 并返回 userId，供 Handler 调用
     */
    public Long validateToken(String token) {
        if (token == null || token.isEmpty()) {
            return null;
        }
        try {
            Object loginId = StpUtil.getLoginIdByToken(token);
            if (loginId == null) {
                return null;
            }
            return Long.parseLong(loginId.toString());
        } catch (Exception e) {
            log.warn("WebSocket token 验证异常: {}", e.getMessage());
            return null;
        }
    }

}
