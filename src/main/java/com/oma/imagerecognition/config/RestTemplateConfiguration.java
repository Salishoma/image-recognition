package com.oma.imagerecognition.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.HttpRequestRetryStrategy;
import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.cookie.StandardCookieSpec;
import org.apache.hc.client5.http.impl.DefaultHttpRequestRetryStrategy;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.core5.util.TimeValue;
import org.apache.hc.core5.util.Timeout;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;


@Slf4j
@Configuration
@RequiredArgsConstructor
public class RestTemplateConfiguration {

    private final RestTemplateProperties restTemplateProperties;

    @Bean
    public RestTemplate restTemplate() {
        HttpRequestRetryStrategy retryStrategy = new DefaultHttpRequestRetryStrategy(
                restTemplateProperties.getRetryCount(), TimeValue.ofSeconds(restTemplateProperties.getRetryIntervalSeconds()));

        ConnectionConfig connectionConfig = ConnectionConfig.custom()
                .setSocketTimeout(Timeout.ofSeconds(restTemplateProperties.getSocketTimeoutSeconds()))
                .build();

        PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();
        connectionManager.setDefaultConnectionConfig(connectionConfig);
        connectionManager.setMaxTotal(restTemplateProperties.getMaxTotalConnections());
        connectionManager.setDefaultMaxPerRoute(restTemplateProperties.getMaxConnectionsPerRoute());

        int requestTimeoutSeconds = restTemplateProperties.getConnectionRequestTimeoutSeconds();
        RequestConfig requestConfig = RequestConfig.custom()
                .setCookieSpec(StandardCookieSpec.IGNORE)
                .setConnectionRequestTimeout(Timeout.ofSeconds(requestTimeoutSeconds))
                .setResponseTimeout(Timeout.ofSeconds(requestTimeoutSeconds))
                .build();

        CloseableHttpClient httpClient = HttpClients.custom()
                .setDefaultRequestConfig(requestConfig)
                .setConnectionManager(connectionManager)
                .setRetryStrategy(retryStrategy)
                .evictExpiredConnections()
                .evictIdleConnections(TimeValue.ofSeconds(restTemplateProperties.getIdleConnectionEvictionSeconds()))
                .build();

        HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory(httpClient);
        factory.setConnectionRequestTimeout((int) Duration.ofSeconds(requestTimeoutSeconds).toMillis());

        return new RestTemplate(factory);
    }

}
