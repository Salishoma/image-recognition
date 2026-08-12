package com.oma.imagerecognition.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "redis", ignoreUnknownFields = false)
@Getter
@Setter
public class RedisProperties {
    private String host;
    private int port;
    private String password;
    private String timeout;
    private boolean sslEnabled;
    private int imageResultExpirationHours;
}
