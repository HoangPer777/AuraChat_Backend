package com.aurachat.module.message.integration;

import com.aurachat.module.message.dto.MessageResponse;
import com.aurachat.module.message.pubsub.MessagePublisher;
import com.redis.testcontainers.RedisContainer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.*;

/**
 * Integration test cho Redis pub/sub.
 * Verify rằng MessagePublisher publish đúng channel và payload có thể serialize/deserialize.
 */
@SpringBootTest
@Testcontainers
class RedisPublishSubscribeTest {

    @Container
    static MongoDBContainer mongoDBContainer = new MongoDBContainer("mongo:7.0");

    @Container
    static RedisContainer redisContainer = new RedisContainer("redis:7.0");

    @DynamicPropertySource
    static void setProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", mongoDBContainer::getReplicaSetUrl);
        registry.add("spring.data.redis.host", redisContainer::getHost);
        registry.add("spring.data.redis.port", () -> redisContainer.getMappedPort(6379));
        registry.add("imagekit.url-endpoint", () -> "https://ik.imagekit.io/test");
        registry.add("imagekit.public-key", () -> "test-public-key");
        registry.add("imagekit.private-key", () -> "test-private-key");
        registry.add("jwt.secret", () -> "test-jwt-secret-32-bytes-minimum-1234567890");
    }

    @Autowired
    private MessagePublisher messagePublisher;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Test
    void messagePublisher_publishesToCorrectChannel() throws InterruptedException {
        String convId = "test-conv-123";
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<String> receivedPayload = new AtomicReference<>();

        // Subscribe trực tiếp qua RedisTemplate để verify
        redisTemplate.getConnectionFactory().getConnection()
            .subscribe((message, pattern) -> {
                receivedPayload.set(new String(message.getBody()));
                latch.countDown();
            }, ("chat:" + convId).getBytes());

        // Publish
        MessageResponse msg = new MessageResponse(
            "msg1", convId, "userA", "TEXT", "Hello", null, null, null,
            List.of(), false, Instant.now()
        );
        messagePublisher.publish(convId, msg);

        // Verify: subscriber nhận được trong 2 giây
        boolean received = latch.await(2, TimeUnit.SECONDS);
        assertThat(received).isTrue();
        assertThat(receivedPayload.get()).contains("msg1");
        assertThat(receivedPayload.get()).contains("Hello");
    }
}
