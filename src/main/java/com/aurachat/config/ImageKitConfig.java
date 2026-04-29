package com.aurachat.config;

import io.imagekit.client.ImageKitClient;
import io.imagekit.client.okhttp.ImageKitOkHttpClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

/**
 * Configuration for ImageKit SDK.
 * Initializes ImageKit client with credentials from environment variables.
 */
@Component
public class ImageKitConfig {

    @Value("${imagekit.private-key}")
    private String privateKey;

    @Bean
    public ImageKitClient imageKit() {
        return ImageKitOkHttpClient.builder()
                .privateKey(privateKey)
                .build();
    }
}
