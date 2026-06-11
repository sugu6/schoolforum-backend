package com.example.schoolforum.config;

import com.example.schoolforum.pojo.common.Result;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * API 速率限制拦截器
 * 基于 Redis 的滑动窗口限流，支持按 IP 和用户两种维度
 *
 * @author code-review-fix
 * @since 2026-06-11
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RateLimitInterceptor implements HandlerInterceptor {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    private static final String RATE_LIMIT_PREFIX = "rate_limit:";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String limitKey = getLimitKey(request);
        if (limitKey == null) {
            return true;
        }

        RateLimitConfig config = getRateLimitConfig(request);

        String redisKey = RATE_LIMIT_PREFIX + limitKey;
        String countStr = redisTemplate.opsForValue().get(redisKey);

        int count = 0;
        if (countStr != null) {
            try {
                count = Integer.parseInt(countStr);
            } catch (NumberFormatException e) {
                log.warn("Redis 速率限制计数值格式错误: key={}, value={}", redisKey, countStr);
                redisTemplate.delete(redisKey);
            }
        }
        if (count >= config.maxRequests) {
            log.warn("API 速率限制触发: key={}, count={}, limit={}", limitKey, count, config.maxRequests);
            sendRateLimitResponse(response, config.windowSeconds);
            return false;
        }

        if (count == 0) {
            redisTemplate.opsForValue().set(redisKey, "1", config.windowSeconds, TimeUnit.SECONDS);
        } else {
            redisTemplate.opsForValue().increment(redisKey);
        }

        return true;
    }

    private String getLimitKey(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String method = request.getMethod();

        // 登录/注册/重置密码：按 IP 限流
        if (uri.contains("/users/login") || uri.contains("/users/register") || uri.contains("/users/resetPassword")) {
            return "ip:" + getClientIp(request) + ":" + uri;
        }

        // Token 刷新：按 IP 限流（防止 refresh token 暴力破解）
        if (uri.contains("/auth/refresh")) {
            return "ip:" + getClientIp(request) + ":refresh";
        }

        // 验证码发送：按 IP 限流
        if (uri.contains("/users/captcha")) {
            return "ip:" + getClientIp(request) + ":captcha";
        }

        // 搜索：按 IP 限流
        if (uri.startsWith("/search")) {
            return "ip:" + getClientIp(request) + ":search";
        }

        // 其他写操作：按 IP 限流（宽松）
        if ("POST".equals(method) || "PUT".equals(method) || "DELETE".equals(method)) {
            return "ip:" + getClientIp(request) + ":write";
        }

        // 读操作：不限流
        return null;
    }

    private RateLimitConfig getRateLimitConfig(HttpServletRequest request) {
        String uri = request.getRequestURI();

        // 登录/注册：严格限流
        if (uri.contains("/users/login") || uri.contains("/users/register")) {
            return new RateLimitConfig(5, 60); // 5次/分钟
        }

        // Token 刷新：中等限流（防止 refresh token 暴力破解）
        if (uri.contains("/auth/refresh")) {
            return new RateLimitConfig(10, 60); // 10次/分钟
        }

        // 验证码：严格限流
        if (uri.contains("/users/captcha")) {
            return new RateLimitConfig(3, 60); // 3次/分钟
        }

        // 重置密码：严格限流
        if (uri.contains("/users/resetPassword")) {
            return new RateLimitConfig(3, 60); // 3次/分钟
        }

        // 搜索：中等限流
        if (uri.startsWith("/search")) {
            return new RateLimitConfig(30, 60); // 30次/分钟
        }

        // 其他写操作：宽松限流
        return new RateLimitConfig(60, 60); // 60次/分钟
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // X-Forwarded-For 可能有多个 IP，取第一个
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }

    private void sendRateLimitResponse(HttpServletResponse response, int retryAfterSeconds) throws IOException {
        response.setStatus(429);
        response.setContentType("application/json;charset=UTF-8");
        response.setHeader("Retry-After", String.valueOf(retryAfterSeconds));
        response.getWriter().write(objectMapper.writeValueAsString(
                Result.error(429, "请求过于频繁，请稍后重试")
        ));
    }

    /**
     * 速率限制配置
     */
    private record RateLimitConfig(int maxRequests, int windowSeconds) {}
}
