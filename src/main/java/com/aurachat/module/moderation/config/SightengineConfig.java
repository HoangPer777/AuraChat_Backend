package com.aurachat.module.moderation.config;

import com.aurachat.module.moderation.service.SightengineImageModerationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SightengineConfig {

    @Bean
    SightengineImageModerationService sightengineImageModerationService(
        ObjectMapper objectMapper,
        SightengineProperties properties
    ) {
        return new SightengineImageModerationService(
            objectMapper,
            properties.getApiUser(),
            properties.getApiSecret(),
            properties.isEnabled(),
            properties.getNudityThreshold()
        );
    }
}
