package com.oma.imagerecognition.config;

import com.oma.imagerecognition.dto.response.SessionImageResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisPassword;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class RedisConfig {

    private final RedisProperties redisProperties;

    @Bean
    public LettuceConnectionFactory redisConnectionFactory() {

        RedisStandaloneConfiguration redisConfig = new RedisStandaloneConfiguration();
        redisConfig.setHostName(redisProperties.getHost());
        redisConfig.setPort(redisProperties.getPort());
        redisConfig.setPassword(RedisPassword.of(redisProperties.getPassword()));

        LettuceClientConfiguration.LettuceClientConfigurationBuilder clientConfigBuilder =
                LettuceClientConfiguration.builder()
                        .commandTimeout(Duration.ofSeconds(5));

        if (redisProperties.isSslEnabled()) {
            clientConfigBuilder.useSsl();
        }

        LettuceClientConfiguration clientConfig = clientConfigBuilder.build();

        return new LettuceConnectionFactory(redisConfig, clientConfig);
    }

    @Bean
    public RedisTemplate<String, SessionImageResult> redisTemplate(RedisConnectionFactory factory) {
        RedisTemplate<String, SessionImageResult> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        return template;
    }
}

