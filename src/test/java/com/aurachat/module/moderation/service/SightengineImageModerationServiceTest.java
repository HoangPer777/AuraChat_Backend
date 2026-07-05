package com.aurachat.module.moderation.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SightengineImageModerationServiceTest {

    private SightengineImageModerationService service;

    @BeforeEach
    void setUp() {
        service = new SightengineImageModerationService(new ObjectMapper(), "test-user", "test-secret", true, 0.5);
    }

    @Test
    void parseResponse_flagsExplicitContent() throws Exception {
        String json = """
            {
              "status": "success",
              "nudity": {
                "sexual_activity": 0.91,
                "sexual_display": 0.05,
                "erotica": 0.02,
                "very_suggestive": 0.1,
                "suggestive": 0.05,
                "mildly_suggestive": 0.01,
                "none": 0.02
              }
            }
            """;

        var result = service.parseResponse(json);

        assertThat(result.sensitive()).isTrue();
        assertThat(result.labels()).anyMatch(label -> label.contains("sexual_activity"));
    }

    @Test
    void parseResponse_allowsSafeImage() throws Exception {
        String json = """
            {
              "status": "success",
              "nudity": {
                "sexual_activity": 0.01,
                "sexual_display": 0.01,
                "erotica": 0.01,
                "very_suggestive": 0.02,
                "suggestive": 0.03,
                "mildly_suggestive": 0.05,
                "none": 0.98
              }
            }
            """;

        var result = service.parseResponse(json);

        assertThat(result.sensitive()).isFalse();
    }

    @Test
    void isConfigured_falseWhenCredentialsMissing() {
        var disabled = new SightengineImageModerationService(new ObjectMapper(), "", "", true, 0.5);
        assertThat(disabled.isConfigured()).isFalse();
    }
}
