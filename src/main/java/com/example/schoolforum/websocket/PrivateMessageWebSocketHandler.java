package com.example.schoolforum.websocket;

import com.example.schoolforum.pojo.PrivateMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 私信 WebSocket 处理器
 * 连接建立后需先发送 {"type":"auth","token":"xxx"} 进行认证
 * 认证成功后才注册会话并接收/发送业务消息
 *
 * @author sugu
 * @since 2026-03-07
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PrivateMessageWebSocketHandler extends TextWebSocketHandler {

    private final ObjectMapper objectMapper;
    private final WebSocketAuthInterceptor authInterceptor;
    private final Map<Long, WebSocketSession> sessionMap = new ConcurrentHashMap<>();

    private static final long AUTH_TIMEOUT_MS = 10_000;

    private static final ScheduledExecutorService authTimeoutScheduler =
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "ws-auth-timeout");
                t.setDaemon(true);
                return t;
            });

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        // 未认证，等待 auth 消息
        log.debug("WebSocket 连接建立(未认证): sessionId={}", session.getId());
        scheduleAuthTimeout(session);
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        Long userId = (Long) session.getAttributes().get(WebSocketAuthInterceptor.USER_ID_KEY);

        // 未认证：只处理 auth 消息
        if (userId == null) {
            handleAuthMessage(session, message);
            return;
        }

        log.debug("收到 WebSocket 消息: userId={}, payload={}", userId, message.getPayload());

        String response = objectMapper.writeValueAsString(Map.of(
                "type", "pong",
                "timestamp", System.currentTimeMillis()
        ));
        session.sendMessage(new TextMessage(response));
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        Long userId = (Long) session.getAttributes().get(WebSocketAuthInterceptor.USER_ID_KEY);
        if (userId != null) {
            sessionMap.remove(userId);
            log.info("WebSocket 连接关闭: userId={}, status={}", userId, status);
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        Long userId = (Long) session.getAttributes().get(WebSocketAuthInterceptor.USER_ID_KEY);
        log.error("WebSocket 传输错误: userId={}, error={}", userId, exception.getMessage());
        if (session.isOpen()) {
            session.close();
        }
    }

    private void handleAuthMessage(WebSocketSession session, TextMessage message) {
        try {
            Map<String, Object> data = objectMapper.readValue(message.getPayload(), Map.class);
            String type = (String) data.get("type");
            if (!"auth".equals(type)) {
                sendAndClose(session, "auth_required", "请先发送认证消息");
                return;
            }

            String token = (String) data.get("token");
            Long userId = authInterceptor.validateToken(token);
            if (userId == null) {
                sendAndClose(session, "auth_error", "无效的 token");
                return;
            }

            session.getAttributes().put(WebSocketAuthInterceptor.USER_ID_KEY, userId);
            registerSession(userId, session);

            String response = objectMapper.writeValueAsString(Map.of(
                    "type", "auth_success",
                    "data", Map.of("userId", userId)
            ));
            session.sendMessage(new TextMessage(response));
            log.info("WebSocket 认证成功: userId={}, sessionId={}", userId, session.getId());
        } catch (Exception e) {
            log.warn("WebSocket auth 消息处理失败: {}", e.getMessage());
            sendAndClose(session, "auth_error", "认证失败");
        }
    }

    private void registerSession(Long userId, WebSocketSession session) {
        WebSocketSession oldSession = sessionMap.put(userId, session);
        if (oldSession != null && oldSession.isOpen() && oldSession != session) {
            try {
                oldSession.close();
            } catch (IOException e) {
                log.warn("关闭旧 WebSocket 会话失败: userId={}", userId);
            }
        }
    }

    private void scheduleAuthTimeout(WebSocketSession session) {
        authTimeoutScheduler.schedule(() -> {
            try {
                Long userId = (Long) session.getAttributes().get(WebSocketAuthInterceptor.USER_ID_KEY);
                if (userId == null && session.isOpen()) {
                    log.warn("WebSocket 认证超时: sessionId={}", session.getId());
                    sendAndClose(session, "auth_timeout", "认证超时");
                }
            } catch (Exception e) {
                // 会话可能已关闭
            }
        }, AUTH_TIMEOUT_MS, TimeUnit.MILLISECONDS);
    }

    private void sendAndClose(WebSocketSession session, String type, String reason) {
        try {
            String msg = objectMapper.writeValueAsString(Map.of(
                    "type", type,
                    "data", Map.of("message", reason)
            ));
            session.sendMessage(new TextMessage(msg));
            session.close(CloseStatus.POLICY_VIOLATION);
        } catch (IOException e) {
            log.warn("发送 WebSocket 消息失败: {}", e.getMessage());
        }
    }

    public void sendMessage(Long userId, PrivateMessage message) {
        WebSocketSession session = sessionMap.get(userId);
        if (session != null && session.isOpen()) {
            try {
                String json = objectMapper.writeValueAsString(Map.of(
                        "type", "private_message",
                        "data", message
                ));
                session.sendMessage(new TextMessage(json));
                log.debug("WebSocket 私信推送成功: userId={}, messageId={}", userId, message.getId());
            } catch (IOException e) {
                log.error("WebSocket 私信推送失败: userId={}, error={}", userId, e.getMessage());
            }
        }
    }

    public void sendUnreadCountUpdate(Long userId, int unreadCount) {
        WebSocketSession session = sessionMap.get(userId);
        if (session != null && session.isOpen()) {
            try {
                String json = objectMapper.writeValueAsString(Map.of(
                        "type", "unread_count_update",
                        "data", Map.of("unreadCount", unreadCount)
                ));
                session.sendMessage(new TextMessage(json));
                log.debug("WebSocket 未读数推送成功: userId={}, count={}", userId, unreadCount);
            } catch (IOException e) {
                log.error("WebSocket 未读数推送失败: userId={}, error={}", userId, e.getMessage());
            }
        }
    }

    public boolean isOnline(Long userId) {
        WebSocketSession session = sessionMap.get(userId);
        return session != null && session.isOpen();
    }

    public int getOnlineCount() {
        return sessionMap.size();
    }

}
