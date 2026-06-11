package com.example.schoolforum.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.example.schoolforum.constant.RedisCacheKey;
import com.example.schoolforum.enums.ActiveStatus;
import com.example.schoolforum.exception.BusinessException;
import com.example.schoolforum.pojo.Users;
import com.example.schoolforum.service.UsersService;
import com.example.schoolforum.util.RefreshTokenCookieUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "认证管理", description = "Token刷新等认证相关接口")
public class AuthController {

    private final StringRedisTemplate redisTemplate;
    private final UsersService usersService;
    private final RefreshTokenCookieUtils refreshTokenCookieUtils;

    @PostMapping("refresh")
    @Operation(summary = "刷新Token", description = "使用 httpOnly Cookie 中的 refreshToken 换取新的 accessToken 和 refreshToken")
    public Map<String, Object> refreshToken(HttpServletRequest request, HttpServletResponse response) {
        // 从 Cookie 中读取 refresh token
        String refreshToken = refreshTokenCookieUtils.getRefreshTokenFromCookie(request);
        if (refreshToken == null || refreshToken.isEmpty()) {
            log.debug("Token刷新失败: Cookie中无refreshToken");
            refreshTokenCookieUtils.clearRefreshTokenCookie(response);
            throw new BusinessException(401, "refreshToken不存在，请重新登录");
        }

        // 从 Redis 查找 refresh token
        String userIdStr = redisTemplate.opsForValue().get(RedisCacheKey.REFRESH_TOKEN + refreshToken);
        if (userIdStr == null) {
            // Refresh token 已过期或已被轮转使用
            log.info("Token刷新失败: refreshToken无效或已过期");
            refreshTokenCookieUtils.clearRefreshTokenCookie(response);
            throw new BusinessException(401, "登录已过期，请重新登录");
        }

        // 删除旧 refresh token（轮转机制，一次性使用）
        redisTemplate.delete(RedisCacheKey.REFRESH_TOKEN + refreshToken);

        Long userId;
        try {
            userId = Long.parseLong(userIdStr);
        } catch (NumberFormatException e) {
            refreshTokenCookieUtils.clearRefreshTokenCookie(response);
            throw new BusinessException("无效的refreshToken");
        }

        // 验证用户是否存在且活跃
        Users user = usersService.getById(userId);
        if (user == null) {
            refreshTokenCookieUtils.clearRefreshTokenCookie(response);
            throw new BusinessException("用户不存在");
        }
        if (user.getIsActive() == ActiveStatus.INACTIVE) {
            refreshTokenCookieUtils.clearRefreshTokenCookie(response);
            throw new BusinessException("账户已被禁用");
        }

        // 生成新的 access token（Sa-Token 自动写入 httpOnly Cookie）
        // 注意：不清除该用户的其他 token，避免多设备/多标签页场景下其他会话被意外登出
        StpUtil.login(userId);
        if (user.getRole() != null) {
            StpUtil.getSession().set("roles",
                    Collections.singletonList(user.getRole().name().toLowerCase()));
        }

        // 生成新的 refresh token（轮转），写入 httpOnly Cookie
        String newRefreshToken = UUID.randomUUID().toString().replace("-", "");
        redisTemplate.opsForValue().set(
                RedisCacheKey.REFRESH_TOKEN + newRefreshToken,
                String.valueOf(userId),
                RedisCacheKey.REFRESH_TOKEN_TTL,
                TimeUnit.DAYS
        );
        refreshTokenCookieUtils.setRefreshTokenCookie(response, newRefreshToken);

        log.info("Token刷新成功: userId={}", userId);
        long expiresIn = StpUtil.getTokenInfo().getTokenTimeout();
        return Map.of("expiresIn", expiresIn);
    }
}
