package com.aurachat.config;

import io.imagekit.client.ImageKitClient;
import io.imagekit.client.okhttp.ImageKitOkHttpClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ImageKitConfig {

    @Value("${imagekit.url-endpoint}")
    private String urlEndpoint;

    @Value("${imagekit.private-key}")
    private String privateKey;

    @Bean
    public ImageKitClient imageKit() {
        return ImageKitOkHttpClient.builder()
            .baseUrl(urlEndpoint)
            .privateKey(privateKey)
            .build();
    }
}