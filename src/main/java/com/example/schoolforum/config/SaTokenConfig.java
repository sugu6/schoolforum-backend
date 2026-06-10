package com.example.schoolforum.config;

import cn.dev33.satoken.context.SaHolder;
import cn.dev33.satoken.exception.SaTokenContextException;
import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.jwt.StpLogicJwtForSimple;
import cn.dev33.satoken.stp.StpLogic;
import cn.dev33.satoken.stp.StpUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import jakarta.annotation.PostConstruct;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class SaTokenConfig implements WebMvcConfigurer {

    @Value("${sa-token.jwt-secret-key:}")
    private String jwtSecretKey;

    @Value("${spring.profiles.active:dev}")
    private String activeProfile;

    @PostConstruct
    public void checkJwtSecret() {
        if ("prod".equals(activeProfile)) {
            if (jwtSecretKey == null || jwtSecretKey.isEmpty()) {
                throw new IllegalStateException("生产环境必须配置 JWT_SECRET_KEY 环境变量");
            }
            if (jwtSecretKey.contains("dev") || jwtSecretKey.contains("do-not-use")) {
                throw new IllegalStateException("生产环境禁止使用开发默认 JWT 密钥，请配置强随机密钥");
            }
        }
    }

    @Bean
    public StpLogic getStpLogicJwt() {
        return new StpLogicJwtForSimple();
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new SaInterceptor(handle -> {
            try {
                SaHolder.getStorage();
                StpUtil.checkLogin();
            } catch (SaTokenContextException e) {
                log.debug("Sa-Token 上下文未初始化，跳过登录检查（可能是异步请求或错误转发）");
            }
        }))
                .addPathPatterns("/**")
                .excludePathPatterns(getExcludePatterns());
    }

    private String[] getExcludePatterns() {
        return new String[] {
                // 首页
                "/",

                // 用户模块 - 公开接口
                "/users/login",
                "/users/register",
                "/users/resetPassword",
                "/users/verifyCaptcha",
                "/users/captcha",
                "/users/getInfo/**",
                "/users/list",
                "/users/logout",

                // 帖子模块 - 公开接口
                "/posts/get/**",
                "/posts/list",
                "/posts/list/hot",
                "/posts/list/latest",
                "/posts/list/essential",
                "/posts/*/related",
                "/posts/user/**",

                // 分类模块 - 公开接口
                "/categories/list/**",
                "/categories/get/**",

                // 标签模块 - 公开接口
                "/tags/list/**",
                "/tags/get/**",

                // 评论模块 - 公开接口
                "/comments/get/**",
                "/comments/list",
                "/comments/list/post/**",
                "/comments/list/hot/**",

                // 关注模块 - 公开接口
                "/follows/following/**",
                "/follows/followers/**",

                // 收藏模块 - 公开接口
                "/favorites/count/**",

                // 公告模块 - 公开接口
                "/announcements/list",
                "/announcements/{id}",

                // 搜索模块 - 公开接口
                "/search/posts/**",
                "/search/users",
                "/search/all",
                "/search/smart",
                "/search/keywords",
                "/search/health",
                "/search/suggest",
                "/search",

                // 统计计数 - 公开接口
                "/count",

                // OAuth 登录
                "/oauth/render/github",
                "/oauth/callback/github",

                // Swagger 文档
                "/swagger-ui/**",
                "/v3/api-docs/**",
                "/swagger-ui.html",

                // WebSocket（路径已从 /ws/ 改为 /api/ 避免广告拦截器）
                "/api/realtime",
                "/api/post-stats",

                // 静态资源 - 头像
                "/avatars/**",

                // 静态资源 - 帖子图片
                "/post-images/**",

                // 其他
                "/favicon.ico",
                "/error"
        };
    }
}
