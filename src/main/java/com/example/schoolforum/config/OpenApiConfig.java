package com.example.schoolforum.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("校园论坛系统 API")
                        .version("1.0.0")
                        .description("校园论坛系统后端接口文档，包含用户管理、帖子管理、评论管理等功能模块")
                        .contact(new Contact()
                                .name("开发团队")
                                .email("dev@example.com"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0")));
    }

    @Bean
    public GroupedOpenApi userApi() {
        return GroupedOpenApi.builder()
                .group("用户管理")
                .pathsToMatch("/users/**")
                .build();
    }

    @Bean
    public GroupedOpenApi postApi() {
        return GroupedOpenApi.builder()
                .group("帖子管理")
                .pathsToMatch("/posts/**")
                .build();
    }

    @Bean
    public GroupedOpenApi commentApi() {
        return GroupedOpenApi.builder()
                .group("评论管理")
                .pathsToMatch("/comments/**")
                .build();
    }
    @Bean
    public GroupedOpenApi oauthApi() {
        return GroupedOpenApi.builder()
                .group("OAuth第三方登录")
                .pathsToMatch("/oauth/**")
                .build();
    }

    @Bean
    public GroupedOpenApi postImageApi() {
        return GroupedOpenApi.builder()
                .group("帖子图片管理")
                .pathsToMatch("/post-images/**")
                .build();
    }

    @Bean
    public GroupedOpenApi searchApi() {
        return GroupedOpenApi.builder()
                .group("搜索管理")
                .pathsToMatch("/search/**")
                .build();
    }

    @Bean
    public GroupedOpenApi messageApi() {
        return GroupedOpenApi.builder()
                .group("私信管理")
                .pathsToMatch("/messages/**")
                .build();
    }

    @Bean
    public GroupedOpenApi favoriteApi() {
        return GroupedOpenApi.builder()
                .group("收藏管理")
                .pathsToMatch("/favorites/**")
                .build();
    }

    @Bean
    public GroupedOpenApi notificationApi() {
        return GroupedOpenApi.builder()
                .group("通知管理")
                .pathsToMatch("/notifications/**")
                .build();
    }
}
