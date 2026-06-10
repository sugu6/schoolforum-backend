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
 * 用于实时推送私信消息
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
        if (userId != null) {
            WebSocketSession oldSession = sessionMap.put(userId, session);
            if (oldSession != null && oldSession.isOpen() && oldSession != session) {
                oldSession.close();
            }
            log.info("WebSocket 连接建立: userId={}, sessionId={}", userId, session.getId());
        }
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
