package com.example.schoolforum;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;


@SpringBootApplication
@EnableScheduling
@MapperScan("com.example.schoolforum.mapper")
public class SchoolforumApplication implements WebMvcConfigurer {

    public static void main(String[] args) {
        SpringApplication.run(SchoolforumApplication.class, args);
    }

    // 路由重定向到/doc.html
    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addRedirectViewController("/", "/swagger-ui.html");
    }
}
