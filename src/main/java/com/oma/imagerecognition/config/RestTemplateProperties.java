package com.oma.imagerecognition.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Setter
@Getter
@Configuration
@ConfigurationProperties(prefix = "rest.template")
public class RestTemplateProperties {

    private int maxTotalConnections;
    private int maxConnectionsPerRoute;
    private int retryCount;
    private int retryIntervalSeconds;
    private int socketTimeoutSeconds;
    private int connectTimeoutSeconds;
    private int connectionRequestTimeoutSeconds;
    private int idleConnectionEvictionSeconds;

}
