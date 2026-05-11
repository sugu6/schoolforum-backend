package com.example.schoolforum.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "file.upload")
public class FileUploadProperties {

    private String avatarPath;

    private String postImagePath;

    private String maxSize;

    private String allowedTypes;
}
