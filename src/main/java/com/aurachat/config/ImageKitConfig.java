package com.aurachat.config;

import io.imagekit.sdk.ImageKit; // Phải có chữ .sdk
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ImageKitConfig {

    @Value("${imagekit.url-endpoint}")
    private String urlEndpoint;

    @Value("${imagekit.public-key}")
    private String publicKey;

    @Value("${imagekit.private-key}")
    private String privateKey;

    @Bean
    public ImageKit imageKit() {
        // Trong bản 3.0.0, constructor nhận 3 tham số String là cách chuẩn nhất
        return new ImageKit(publicKey, privateKey, urlEndpoint);
    }
}