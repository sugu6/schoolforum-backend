package com.example.schoolforum.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "oauth.github")
public class GitHubOAuthProperties {

    private String clientId;

    private String clientSecret;

    private String redirectUri;

    private boolean disableSsl = false;
}
