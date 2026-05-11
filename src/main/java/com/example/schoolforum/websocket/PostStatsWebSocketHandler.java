package com.example.schoolforum.websocket;

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
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class PostStatsWebSocketHandler extends TextWebSocketHandler {

    private final ObjectMapper objectMapper;

    private final Map<Long, Set<WebSocketSession>> postSubscriptions = new ConcurrentHashMap<>();

    private final Map<WebSocketSession, Long> sessionToPostId = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        log.debug("WebSocket 连接建立: sessionId={}", session.getId());
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();

        try {
            Map<String, Object> data = objectMapper.readValue(payload, Map.class);
            String action = (String) data.get("action");
            Long postId = data.get("postId") != null ? Long.valueOf(data.get("postId").toString()) : null;

            if ("subscribe".equals(action) && postId != null) {
                subscribeToPost(session, postId);
                sendAck(session, "subscribed", postId);
            } else if ("unsubscribe".equals(action) && postId != null) {
                unsubscribeFromPost(session, postId);
                sendAck(session, "unsubscribed", postId);
            } else {
                sendPong(session);
            }
        } catch (Exception e) {
            log.warn("解析 WebSocket 消息失败: payload={}", payload);
            sendPong(session);
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        log.error("WebSocket 传输错误: sessionId={}, error={}", session.getId(), exception.getMessage());
        if (session.isOpen()) {
            session.close();
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        Long postId = sessionToPostId.remove(session);
        if (postId != null) {
            Set<WebSocketSession> sessions = postSubscriptions.get(postId);
            if (sessions != null) {
                sessions.remove(session);
                if (sessions.isEmpty()) {
                    postSubscriptions.remove(postId);
                }
            }
        }
        log.debug("WebSocket 连接关闭: sessionId={}, status={}", session.getId(), status);
    }

    private void subscribeToPost(WebSocketSession session, Long postId) {
        sessionToPostId.put(session, postId);
        postSubscriptions.computeIfAbsent(postId, k -> ConcurrentHashMap.newKeySet()).add(session);
        log.debug("订阅帖子: sessionId={}, postId={}", session.getId(), postId);
    }

    private void sendAck(WebSocketSession session, String action, Long postId) throws IOException {
        String json = objectMapper.writeValueAsString(Map.of(
                "type", "ack",
                "action", action,
                "postId", postId,
                "timestamp", System.currentTimeMillis()
        ));
        session.sendMessage(new TextMessage(json));
    }

    private void unsubscribeFromPost(WebSocketSession session, Long postId) {
        sessionToPostId.remove(session);
        Set<WebSocketSession> sessions = postSubscriptions.get(postId);
        if (sessions != null) {
            sessions.remove(session);
        }
        log.debug("取消订阅帖子: sessionId={}, postId={}", session.getId(), postId);
    }

    private void sendPong(WebSocketSession session) throws IOException {
        String json = objectMapper.writeValueAsString(Map.of(
                "type", "pong",
                "timestamp", System.currentTimeMillis()
        ));
        session.sendMessage(new TextMessage(json));
    }

    public void broadcastStatsUpdate(Long postId, Integer viewCount, Integer likeCount, Integer commentCount) {
        Set<WebSocketSession> sessions = postSubscriptions.get(postId);
        if (sessions == null || sessions.isEmpty()) {
            return;
        }

        String json;
        try {
            json = objectMapper.writeValueAsString(Map.of(
                    "type", "stats_update",
                    "data", Map.of(
                            "postId", postId,
                            "viewCount", viewCount,
                            "likeCount", likeCount,
                            "commentCount", commentCount
                    ),
                    "timestamp", System.currentTimeMillis()
            ));
        } catch (Exception e) {
            log.error("序列化统计数据失败: postId={}", postId, e);
            return;
        }

        TextMessage message = new TextMessage(json);
        for (WebSocketSession session : sessions) {
            if (session.isOpen()) {
                try {
                    session.sendMessage(message);
                } catch (IOException e) {
                    log.error("推送统计数据失败: sessionId={}, postId={}", session.getId(), postId, e);
                }
            }
        }

        log.debug("广播帖子统计更新: postId={}, viewCount={}, likeCount={}, commentCount={}",
                postId, viewCount, likeCount, commentCount);
    }

    public void broadcastCommentCount(Long postId, Integer commentCount) {
        Set<WebSocketSession> sessions = postSubscriptions.get(postId);
        if (sessions == null || sessions.isEmpty()) {
            return;
        }

        String json;
        try {
            json = objectMapper.writeValueAsString(Map.of(
                    "type", "comment_count_update",
                    "data", Map.of(
                            "postId", postId,
                            "commentCount", commentCount
                    ),
                    "timestamp", System.currentTimeMillis()
            ));
        } catch (Exception e) {
            log.error("序列化评论数失败: postId={}", postId, e);
            return;
        }

        TextMessage message = new TextMessage(json);
        for (WebSocketSession session : sessions) {
            if (session.isOpen()) {
                try {
                    session.sendMessage(message);
                } catch (IOException e) {
                    log.error("推送评论数失败: sessionId={}, postId={}", session.getId(), postId, e);
                }
            }
        }

        log.debug("广播评论数更新: postId={}, commentCount={}", postId, commentCount);
    }

    public void broadcastLikeCount(Long postId, Integer likeCount) {
        Set<WebSocketSession> sessions = postSubscriptions.get(postId);
        if (sessions == null || sessions.isEmpty()) {
            return;
        }

        String json;
        try {
            json = objectMapper.writeValueAsString(Map.of(
                    "type", "like_count_update",
                    "data", Map.of(
                            "postId", postId,
                            "likeCount", likeCount
                    ),
                    "timestamp", System.currentTimeMillis()
            ));
        } catch (Exception e) {
            log.error("序列化点赞数失败: postId={}", postId, e);
            return;
        }

        TextMessage message = new TextMessage(json);
        for (WebSocketSession session : sessions) {
            if (session.isOpen()) {
                try {
                    session.sendMessage(message);
                } catch (IOException e) {
                    log.error("推送点赞数失败: sessionId={}, postId={}", session.getId(), postId, e);
                }
            }
        }

        log.debug("广播点赞数更新: postId={}, likeCount={}", postId, likeCount);
    }

    public void broadcastViewCount(Long postId, Integer viewCount) {
        Set<WebSocketSession> sessions = postSubscriptions.get(postId);
        if (sessions == null || sessions.isEmpty()) {
            return;
        }

        String json;
        try {
            json = objectMapper.writeValueAsString(Map.of(
                    "type", "view_count_update",
                    "data", Map.of(
                            "postId", postId,
                            "viewCount", viewCount
                    ),
                    "timestamp", System.currentTimeMillis()
            ));
        } catch (Exception e) {
            log.error("序列化浏览量失败: postId={}", postId, e);
            return;
        }

        TextMessage message = new TextMessage(json);
        for (WebSocketSession session : sessions) {
            if (session.isOpen()) {
                try {
                    session.sendMessage(message);
                } catch (IOException e) {
                    log.error("推送浏览量失败: sessionId={}, postId={}", session.getId(), postId, e);
                }
            }
        }

        log.debug("广播浏览量更新: postId={}, viewCount={}", postId, viewCount);
    }

    public int getSubscriberCount(Long postId) {
        Set<WebSocketSession> sessions = postSubscriptions.get(postId);
        return sessions != null ? sessions.size() : 0;
    }

    public int getTotalSubscriptions() {
        return postSubscriptions.values().stream().mapToInt(Set::size).sum();
    }
}
