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

/**
 * 私信 WebSocket 处理器
 * 认证通过 httpOnly Cookie 在握手阶段完成（WebSocketAuthInterceptor）
 * 连接建立后即可接收/发送业务消息
 *
 * @author sugu
 * @since 2026-03-07
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PrivateMessageWebSocketHandler extends TextWebSocketHandler {

    private final ObjectMapper objectMapper;
    private final Map<Long, WebSocketSession> sessionMap = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        Long userId = (Long) session.getAttributes().get(WebSocketAuthInterceptor.USER_ID_KEY);

        // Cookie 预认证（握手时已从 httpOnly Cookie 读取 token），userId 必定不为 null
        registerSession(userId, session);
        String response = objectMapper.writeValueAsString(Map.of(
                "type", "auth_success",
                "data", Map.of("userId", userId)
        ));
        session.sendMessage(new TextMessage(response));
        log.info("WebSocket Cookie 认证成功: userId={}, sessionId={}", userId, session.getId());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        Long userId = (Long) session.getAttributes().get(WebSocketAuthInterceptor.USER_ID_KEY);

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
