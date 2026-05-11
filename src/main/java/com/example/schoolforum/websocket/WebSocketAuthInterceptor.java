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
 * WebSocket 认证拦截器
 * 在 WebSocket 握手时验证用户身份
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
            String token = servletRequest.getServletRequest().getParameter("token");
            
            if (token == null || token.isEmpty()) {
                log.warn("WebSocket 握手失败: 缺少 token 参数");
                return false;
            }

            try {
                Object loginId = StpUtil.getLoginIdByToken(token);
                if (loginId == null) {
                    log.warn("WebSocket 握手失败: 无效的 token");
                    return false;
                }

                Long userId = Long.parseLong(loginId.toString());
                attributes.put(USER_ID_KEY, userId);
                log.debug("WebSocket 握手成功: userId={}", userId);
                return true;
            } catch (Exception e) {
                log.warn("WebSocket 握手失败: token 验证异常 - {}", e.getMessage());
                return false;
            }
        }

        return false;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response, 
                                WebSocketHandler wsHandler, Exception exception) {
        if (exception != null) {
            log.error("WebSocket 握手异常: {}", exception.getMessage());
        }
    }

}
