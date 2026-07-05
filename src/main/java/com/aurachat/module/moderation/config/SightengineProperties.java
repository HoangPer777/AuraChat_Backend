package com.aurachat.module.moderation.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "sightengine")
public class SightengineProperties {

    private String apiUser = "";
    private String apiSecret = "";
    private boolean enabled = true;
    private double nudityThreshold = 0.55;
}
