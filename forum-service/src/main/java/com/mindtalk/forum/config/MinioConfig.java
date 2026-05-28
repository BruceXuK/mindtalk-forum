package com.mindtalk.forum.config;

import io.minio.MinioClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MinIO 客户端配置
 */
@Configuration
public class MinioConfig {

    private final String endpoint;
    private final String publicEndpoint;
    private final String accessKey;
    private final String secretKey;

    public MinioConfig(@Value("${minio.endpoint}") String endpoint,
                       @Value("${minio.public-endpoint:${minio.endpoint}}") String publicEndpoint,
                       @Value("${minio.access-key}") String accessKey,
                       @Value("${minio.secret-key}") String secretKey) {
        this.endpoint = endpoint;
        this.publicEndpoint = publicEndpoint;
        this.accessKey = accessKey;
        this.secretKey = secretKey;
    }

    @Bean
    public MinioClient minioClient() {
        return MinioClient.builder()
                .endpoint(endpoint)
                .credentials(accessKey, secretKey)
                .build();
    }

    public String getPublicEndpoint() {
        return publicEndpoint;
    }

    public String getInternalEndpoint() {
        return endpoint;
    }
}
