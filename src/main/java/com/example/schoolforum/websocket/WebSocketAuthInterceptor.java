package com.example.schoolforum.websocket;

import cn.dev33.satoken.stp.StpUtil;
import jakarta.servlet.http.Cookie;
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
 * 从 httpOnly Cookie 中读取 Access Token 进行认证
 * 浏览器在 WebSocket 握手时会自动携带同域 Cookie
 * 认证失败则拒绝握手
 *
 * @author sugu
 * @since 2026-03-07
 */
@Slf4j
@Component
public class WebSocketAuthInterceptor implements HandshakeInterceptor {

    public static final String USER_ID_KEY = "userId";
    private static final String AUTH_COOKIE_NAME = "Authorization";

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                    WebSocketHandler wsHandler, Map<String, Object> attributes) throws Exception {
        if (request instanceof ServletServerHttpRequest servletRequest) {
            // 从 Cookie 中读取 Access Token
            String token = getTokenFromCookie(servletRequest);

            if (token != null && !token.isEmpty()) {
                // 去掉 "Bearer " 前缀
                String tokenValue = token.startsWith("Bearer ") ? token.substring(7) : token;
                try {
                    Object loginId = StpUtil.getLoginIdByToken(tokenValue);
                    if (loginId != null) {
                        Long userId = Long.parseLong(loginId.toString());
                        attributes.put(USER_ID_KEY, userId);
                        log.debug("WebSocket 握手认证成功（Cookie方式）: userId={}", userId);
                        return true;
                    }
                } catch (Exception e) {
                    log.debug("WebSocket Cookie token 验证失败: {}", e.getMessage());
                }
            }

            // 无有效 Cookie token，拒绝握手
            log.warn("WebSocket 握手拒绝: 无有效 Cookie token");
            response.setStatusCode(org.springframework.http.HttpStatus.UNAUTHORIZED);
            return false;
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
     * 从 Cookie 中读取 Authorization token
     */
    private String getTokenFromCookie(ServletServerHttpRequest servletRequest) {
        Cookie[] cookies = servletRequest.getServletRequest().getCookies();
        if (cookies == null) return null;
        for (Cookie cookie : cookies) {
            if (AUTH_COOKIE_NAME.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }

}
