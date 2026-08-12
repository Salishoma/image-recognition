package com.oma.imagerecognition.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "rekognition.image", ignoreUnknownFields = false)
@Getter
@Setter
public class RekognitionImageProperties {
    private int maxSize;
}
