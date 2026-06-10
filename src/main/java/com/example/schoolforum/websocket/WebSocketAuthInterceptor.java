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
 * 支持两种 token 传递方式：
 * 1. 路径参数（推荐）：/ws/message/{token}，避免广告拦截器拦截 ?token= 参数
 * 2. 查询参数（兼容）：/ws/message?token=xxx
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
            String token = extractToken(servletRequest);

            if (token == null || token.isEmpty()) {
                log.warn("WebSocket 握手失败: 缺少 token");
                response.setStatusCode(org.springframework.http.HttpStatus.UNAUTHORIZED);
                return false;
            }

            try {
                Object loginId = StpUtil.getLoginIdByToken(token);
                if (loginId == null) {
                    log.warn("WebSocket 握手失败: 无效的 token");
                    response.setStatusCode(org.springframework.http.HttpStatus.UNAUTHORIZED);
                    return false;
                }

                Long userId = Long.parseLong(loginId.toString());
                attributes.put(USER_ID_KEY, userId);
                log.info("WebSocket 握手成功: userId={}", userId);
                return true;
            } catch (Exception e) {
                log.warn("WebSocket 握手失败: token 验证异常 - {}", e.getMessage());
                response.setStatusCode(org.springframework.http.HttpStatus.UNAUTHORIZED);
                return false;
            }
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
     * 从请求中提取 token
     * 优先从路径提取（/ws/message/{token}），其次从查询参数提取（?token=xxx）
     */
    private String extractToken(ServletServerHttpRequest servletRequest) {
        // 1. 从路径中提取：/ws/message/{token}
        String uri = servletRequest.getURI().getPath();
        String prefix = "/ws/message/";
        int idx = uri.indexOf(prefix);
        if (idx >= 0) {
            String tokenFromPath = uri.substring(idx + prefix.length());
            if (!tokenFromPath.isEmpty()) {
                return tokenFromPath;
            }
        }

        // 2. 兼容：从查询参数提取
        return servletRequest.getServletRequest().getParameter("token");
    }

}
