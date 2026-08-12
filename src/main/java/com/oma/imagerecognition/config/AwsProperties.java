package com.oma.imagerecognition.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;

@Configuration
@ConfigurationProperties(prefix = "aws", ignoreUnknownFields = false)
@Getter
@Setter
public class AwsProperties {

    private String accessKey;
    private String secretKey;
    private Region region;
    private String endpoint;
    private String s3BucketName;
    private double threshold;
    private long sessionExpirationTime;
    private long imageExpirationTime;
    private int callAttemptTimeout;
    private int callTimeout;
    private int numRetries;

}