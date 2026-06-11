package com.example.schoolforum.util;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Refresh Token Cookie 工具类
 * 
 * Access Token 由 Sa-Token 自动管理（httpOnly Cookie），无需手动处理。
 * Refresh Token 不由 Sa-Token 管理，需要手动设置/读取/清除 httpOnly Cookie。
 *
 * @author sugu
 */
@Slf4j
@Component
public class RefreshTokenCookieUtils {

    public static final String REFRESH_TOKEN_COOKIE = "RefreshToken";
    public static final String ACCESS_TOKEN_COOKIE = "Authorization";
    public static final long REFRESH_TOKEN_MAX_AGE = 7 * 24 * 60 * 60L; // 7 天
    public static final String REFRESH_TOKEN_PATH = "/api/auth";

    @Value("${sa-token.cookie.secure:false}")
    private boolean cookieSecure;

    @Value("${sa-token.cookie.same-site:Lax}")
    private String cookieSameSite;

    /**
     * 设置 Refresh Token Cookie（httpOnly + Secure + SameSite=Lax）
     */
    public void setRefreshTokenCookie(HttpServletResponse response, String refreshToken) {
        ResponseCookie cookie = ResponseCookie.from(REFRESH_TOKEN_COOKIE, refreshToken)
                .path(REFRESH_TOKEN_PATH)
                .httpOnly(true)
                .sameSite(cookieSameSite)
                .secure(cookieSecure)
                .maxAge(Duration.ofSeconds(REFRESH_TOKEN_MAX_AGE))
                .build();
        response.addHeader("Set-Cookie", cookie.toString());
        log.debug("设置 RefreshToken Cookie: secure={}, path={}", cookieSecure, REFRESH_TOKEN_PATH);
    }

    /**
     * 清除 Refresh Token Cookie
     * 同时清除旧 path (/auth) 的 Cookie，兼容 Cookie path 从 /auth 迁移到 /api/auth 的用户
     */
    public void clearRefreshTokenCookie(HttpServletResponse response) {
        // 清除新 path (/api/auth) 的 Cookie
        ResponseCookie cookie = ResponseCookie.from(REFRESH_TOKEN_COOKIE, "")
                .path(REFRESH_TOKEN_PATH)
                .httpOnly(true)
                .sameSite(cookieSameSite)
                .secure(cookieSecure)
                .maxAge(Duration.ZERO)
                .build();
        response.addHeader("Set-Cookie", cookie.toString());

        // 兼容：清除旧 path (/auth) 的 Cookie
        ResponseCookie legacyCookie = ResponseCookie.from(REFRESH_TOKEN_COOKIE, "")
                .path("/auth")
                .httpOnly(true)
                .sameSite(cookieSameSite)
                .secure(cookieSecure)
                .maxAge(Duration.ZERO)
                .build();
        response.addHeader("Set-Cookie", legacyCookie.toString());
    }

    /**
     * 清除 Access Token Cookie
     * JWT Simple 模式下 StpUtil.logout() 可能只将 token 加入黑名单，不清除 Cookie，需要显式清除
     */
    public void clearAccessTokenCookie(HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from(ACCESS_TOKEN_COOKIE, "")
                .path("/")
                .httpOnly(true)
                .sameSite(cookieSameSite)
                .secure(cookieSecure)
                .maxAge(Duration.ZERO)
                .build();
        response.addHeader("Set-Cookie", cookie.toString());
        log.debug("清除 Access Token Cookie");
    }

    /**
     * 从 Cookie 中读取 Refresh Token
     */
    public String getRefreshTokenFromCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) return null;
        for (Cookie cookie : cookies) {
            if (REFRESH_TOKEN_COOKIE.equals(cookie.getName())) {
                String value = cookie.getValue();
                return (value != null && !value.isEmpty()) ? value : null;
            }
        }
        return null;
    }
}
