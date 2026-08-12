package com.oma.imagerecognition.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.core.retry.RetryPolicy;
import software.amazon.awssdk.services.rekognition.RekognitionClient;
import software.amazon.awssdk.services.s3.S3Client;

import java.time.Duration;

@Configuration
@RequiredArgsConstructor
public class AwsConfig {
    private final AwsProperties aws;

    @Bean
    public S3Client s3Client() {
        return S3Client.builder()
                .region(aws.getRegion())
                .build();
    }

    @Bean
    public RekognitionClient amazonRekognitionClient() {
        return RekognitionClient.builder()
                .region(aws.getRegion())
                .credentialsProvider(DefaultCredentialsProvider.builder()
                        .build())
                .overrideConfiguration(client -> client
                        .apiCallAttemptTimeout(Duration.ofSeconds(aws.getCallAttemptTimeout()))
                        .apiCallTimeout(Duration.ofSeconds(aws.getCallTimeout()))
                        .retryPolicy(RetryPolicy.builder()
                                .numRetries(aws.getNumRetries())
                                .build()))
                .build();
    }
}
