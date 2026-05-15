package com.aurachat.config;

import io.imagekit.sdk.ImageKit;
import io.imagekit.sdk.config.Configuration;
import io.imagekit.sdk.utils.Utils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;

@org.springframework.context.annotation.Configuration
public class ImageKitConfig {

    @Value("${imagekit.url-endpoint}")
    private String urlEndpoint;

    @Value("${imagekit.public-key}")
    private String publicKey;

    @Value("${imagekit.private-key}")
    private String privateKey;

    @Bean
    public ImageKit imageKit() {
        Configuration config = new Configuration(publicKey, privateKey, urlEndpoint);
        return ImageKit.getInstance(config);
    }
}