package com.example.schoolforum.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * SSE 连接管理器
 * 用于管理用户的 SSE 连接，实现实时消息推送
 *
 * @author sugu
 * @since 2026-03-06
 */
@Slf4j
@Component
public class SseEmitterManager {

    private final Map<Long, SseEmitter> emitterMap = new ConcurrentHashMap<>();

    /**
     * 创建并添加 SSE 连接
     *
     * @param userId 用户ID
     * @return SseEmitter
     */
    public SseEmitter createEmitter(Long userId) {
        SseEmitter emitter = new SseEmitter(0L);
        
        emitter.onCompletion(() -> {
            log.debug("SSE 连接完成: userId={}", userId);
            emitterMap.remove(userId);
        });
        
        emitter.onTimeout(() -> {
            log.debug("SSE 连接超时: userId={}", userId);
            emitterMap.remove(userId);
        });
        
        emitter.onError(e -> {
            log.debug("SSE 连接错误: userId={}, error={}", userId, e.getMessage());
            emitterMap.remove(userId);
        });
        
        SseEmitter oldEmitter = emitterMap.put(userId, emitter);
        if (oldEmitter != null) {
            oldEmitter.complete();
        }
        
        log.debug("SSE 连接建立: userId={}", userId);
        return emitter;
    }

    /**
     * 向指定用户推送消息
     *
     * @param userId 用户ID
     * @param data   消息数据
     */
    public void sendToUser(Long userId, Object data) {
        SseEmitter emitter = emitterMap.get(userId);
        if (emitter != null) {
            try {
                emitter.send(SseEmitter.event()
                        .name("notification")
                        .data(data));
                log.debug("SSE 消息推送成功: userId={}", userId);
            } catch (IOException e) {
                log.error("SSE 消息推送失败: userId={}, error={}", userId, e.getMessage());
                emitterMap.remove(userId);
            }
        }
    }

    /**
     * 向所有用户推送消息
     *
     * @param data 消息数据
     */
    public void sendToAll(Object data) {
        emitterMap.forEach((userId, emitter) -> {
            try {
                emitter.send(SseEmitter.event()
                        .name("notification")
                        .data(data));
            } catch (IOException e) {
                log.error("SSE 消息推送失败: userId={}, error={}", userId, e.getMessage());
                emitterMap.remove(userId);
            }
        });
    }

    /**
     * 移除用户连接
     *
     * @param userId 用户ID
     */
    public void removeEmitter(Long userId) {
        SseEmitter emitter = emitterMap.remove(userId);
        if (emitter != null) {
            emitter.complete();
        }
    }

    /**
     * 检查用户是否在线
     *
     * @param userId 用户ID
     * @return 是否在线
     */
    public boolean isOnline(Long userId) {
        return emitterMap.containsKey(userId);
    }

    /**
     * 获取在线用户数量
     *
     * @return 在线用户数量
     */
    public int getOnlineCount() {
        return emitterMap.size();
    }
}
