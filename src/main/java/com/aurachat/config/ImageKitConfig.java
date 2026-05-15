package com.aurachat.config;

import io.imagekit.sdk.ImageKit; // ĐÚNG: Import class của thư viện
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
    public ImageKit imageKit() { // ĐÚNG: Kiểu trả về phải là ImageKit
        // Khởi tạo đối tượng của thư viện ImageKit
        return new ImageKit(publicKey, privateKey, urlEndpoint);
    }
}