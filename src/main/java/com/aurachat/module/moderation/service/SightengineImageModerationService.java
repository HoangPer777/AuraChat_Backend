package com.aurachat.module.moderation.service;

import com.aurachat.module.moderation.dto.ImageModerationResult;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Slf4j
public class SightengineImageModerationService {

    private static final String CHECK_URL = "https://api.sightengine.com/1.0/check.json";
    private static final List<String> NUDITY_CLASSES = List.of(
        "sexual_activity",
        "sexual_display",
        "erotica",
        "very_suggestive",
        "suggestive"
    );

    private final ObjectMapper objectMapper;
    private final String apiUser;
    private final String apiSecret;
    private final boolean enabled;
    private final double nudityThreshold;
    private final HttpClient httpClient;

    public SightengineImageModerationService(
        ObjectMapper objectMapper,
        String apiUser,
        String apiSecret,
        boolean enabled,
        double nudityThreshold
    ) {
        this.objectMapper = objectMapper;
        this.apiUser = apiUser == null ? "" : apiUser.trim();
        this.apiSecret = apiSecret == null ? "" : apiSecret.trim();
        this.enabled = enabled;
        this.nudityThreshold = nudityThreshold;
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    }

    public boolean isConfigured() {
        return enabled && !apiUser.isBlank() && !apiSecret.isBlank();
    }

    public ImageModerationResult checkImageUrl(String imageUrl) {
        if (!isConfigured() || imageUrl == null || imageUrl.isBlank()) {
            return ImageModerationResult.safe();
        }

        try {
            URI uri = UriComponentsBuilder.fromHttpUrl(CHECK_URL)
                .queryParam("url", imageUrl)
                .queryParam("models", "nudity-2.1")
                .queryParam("api_user", apiUser)
                .queryParam("api_secret", apiSecret)
                .build(true)
                .toUri();

            HttpRequest request = HttpRequest.newBuilder(uri)
                .GET()
                .timeout(Duration.ofSeconds(20))
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                log.warn("Sightengine HTTP {} for image moderation", response.statusCode());
                return ImageModerationResult.safe();
            }
            return parseResponse(response.body());
        } catch (Exception ex) {
            log.warn("Sightengine image check failed: {}", ex.getMessage());
            return ImageModerationResult.safe();
        }
    }

    ImageModerationResult parseResponse(String json) throws Exception {
        JsonNode root = objectMapper.readTree(json);
        if (!"success".equalsIgnoreCase(root.path("status").asText())) {
            return ImageModerationResult.safe();
        }

        JsonNode nudity = root.path("nudity");
        List<String> labels = new ArrayList<>();
        double maxScore = 0;

        for (String clazz : NUDITY_CLASSES) {
            double score = nudity.path(clazz).asDouble(0);
            if (score >= nudityThreshold) {
                labels.add(clazz + ":" + String.format(Locale.US, "%.2f", score));
            }
            maxScore = Math.max(maxScore, score);
        }

        boolean sensitive = !labels.isEmpty();
        String summary = sensitive
            ? "nudity score max=" + String.format(Locale.US, "%.2f", maxScore)
            : "safe";
        return new ImageModerationResult(sensitive, labels, summary);
    }
}
