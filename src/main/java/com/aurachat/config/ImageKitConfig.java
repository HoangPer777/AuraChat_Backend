package com.aurachat.config;

import io.imagekit.client.ImageKitClient;
import io.imagekit.client.okhttp.ImageKitOkHttpClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for ImageKit client.
 * Initializes ImageKitClient with credentials from application properties.
 * 
 * Note: The public key is not needed for server-side operations.
 * It's only used for client-side URL generation and uploads.
 */
@Configuration
public class ImageKitConfig {

    @Value("${imagekit.url-endpoint}")
    private String urlEndpoint;

    @Value("${imagekit.public-key}")
    private String publicKey;

    @Value("${imagekit.private-key}")
    private String privateKey;

    /**
     * Creates and configures ImageKitClient bean.
     * Uses ImageKitOkHttpClient builder pattern as per SDK 3.0.0 documentation.
     * Only privateKey is required for server-side file operations.
     * 
     * @return configured ImageKitClient instance
     */
    @Bean
    public ImageKitClient imageKitClient() {
        return ImageKitOkHttpClient.builder()
            .privateKey(privateKey)
            .build();
    }
}
