package com.aurachat.module.presence.integration;

import com.aurachat.module.presence.service.PresenceService;
import com.redis.testcontainers.RedisContainer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.*;

/**
 * Integration tests cho Presence module với Redis thật (Testcontainers).
 */
@SpringBootTest
@Testcontainers
class PresenceIntegrationTest {

    @Container
    static MongoDBContainer mongoDBContainer = new MongoDBContainer("mongo:7.0");

    @Container
    static RedisContainer redisContainer = new RedisContainer("redis:7.0");

    @DynamicPropertySource
    static void setProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", mongoDBContainer::getReplicaSetUrl);
        registry.add("spring.data.redis.host", redisContainer::getHost);
        registry.add("spring.data.redis.port", () -> redisContainer.getMappedPort(6379));
    }

    @Autowired
    private PresenceService presenceService;

    @Test
    void updatePresence_online_setsKeyInRealRedis() {
        String userId = "integration-user-1";

        presenceService.updatePresence(userId, "online");

        assertThat(presenceService.isOnline(userId)).isTrue();
    }

    @Test
    void updatePresence_offline_deletesKeyFromRealRedis() {
        String userId = "integration-user-2";

        presenceService.updatePresence(userId, "online");
        assertThat(presenceService.isOnline(userId)).isTrue();

        presenceService.updatePresence(userId, "offline");
        assertThat(presenceService.isOnline(userId)).isFalse();
    }

    @Test
    void refreshTTL_extendsKeyInRealRedis() throws InterruptedException {
        String userId = "integration-user-3";

        presenceService.updatePresence(userId, "online");
        assertThat(presenceService.isOnline(userId)).isTrue();

        // Heartbeat gia hạn TTL
        presenceService.refreshTTL(userId);

        // Key vẫn tồn tại sau heartbeat
        assertThat(presenceService.isOnline(userId)).isTrue();
    }
}
