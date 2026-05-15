package com.aurachat.module.friend.integration;

import com.aurachat.config.FirebaseConfig;
import com.aurachat.module.auth.entity.User;
import com.aurachat.module.auth.repository.UserRepository;
import com.aurachat.module.friend.controller.FriendWebSocketController;
import com.aurachat.module.friend.dto.FriendRequestDto;
import com.aurachat.module.friend.dto.SendFriendRequestDto;
import com.aurachat.module.friend.entity.FriendRequest;
import com.aurachat.module.friend.entity.Friendship;
import com.aurachat.module.friend.repository.FriendRequestRepository;
import com.aurachat.module.friend.repository.FriendshipRepository;
import com.aurachat.module.friend.service.FriendService;
import com.redis.testcontainers.RedisContainer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Integration tests cho Friend module với MongoDB và Redis thật (Testcontainers).
 */
@SpringBootTest
@Testcontainers
class FriendIntegrationTest {

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
    private FriendService friendService;
    @Autowired
    private FriendRequestRepository friendRequestRepository;
    @Autowired
    private FriendshipRepository friendshipRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private StringRedisTemplate redisTemplate;

    @MockBean
    private FriendWebSocketController friendWebSocketController;
    @MockBean
    private FirebaseConfig firebaseConfig;

    @AfterEach
    void cleanup() {
        friendRequestRepository.deleteAll();
        friendshipRepository.deleteAll();
        userRepository.deleteAll();
        if (redisTemplate.getConnectionFactory() != null) {
            redisTemplate.getConnectionFactory().getConnection().flushAll();
        }
    }

    private User buildUser(String id, String displayName) {
        return User.builder()
            .id(id)
            .displayName(displayName)
            .email(id + "@example.com")
            .avatarUrl(null)
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();
    }

    // ─── sendFriendRequest + getPendingRequests ─────────────────────────────

    @Test
    void sendFriendRequest_createsPendingRequestAndPendingList() {
        userRepository.saveAll(List.of(
            buildUser("userA", "Alice"),
            buildUser("userB", "Bob")
        ));

        FriendRequestDto request = friendService.sendFriendRequest("userA", new SendFriendRequestDto("userB"));

        FriendRequest saved = friendRequestRepository.findById(request.id()).orElseThrow();
        assertThat(saved.getStatus()).isEqualTo("PENDING");

        List<FriendRequestDto> pending = friendService.getPendingRequests("userB");
        assertThat(pending).hasSize(1);
        assertThat(pending.get(0).id()).isEqualTo(request.id());

        verify(friendWebSocketController).notifyFriendRequestCreated(eq("userB"), any());
    }

    // ─── acceptFriendRequest ────────────────────────────────────────────────

    @Test
    void acceptFriendRequest_createsBidirectionalFriendship() {
        userRepository.saveAll(List.of(
            buildUser("userA", "Alice"),
            buildUser("userB", "Bob")
        ));

        FriendRequestDto request = friendService.sendFriendRequest("userA", new SendFriendRequestDto("userB"));
        friendService.acceptFriendRequest("userB", request.id());

        assertThat(friendshipRepository.findByUserId("userA"))
            .extracting(Friendship::getFriendId)
            .containsExactly("userB");
        assertThat(friendshipRepository.findByUserId("userB"))
            .extracting(Friendship::getFriendId)
            .containsExactly("userA");

        FriendRequest updated = friendRequestRepository.findById(request.id()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo("ACCEPTED");
    }

    // ─── declineFriendRequest ───────────────────────────────────────────────

    @Test
    void declineFriendRequest_marksDeclinedAndKeepsNoFriendship() {
        userRepository.saveAll(List.of(
            buildUser("userA", "Alice"),
            buildUser("userB", "Bob")
        ));

        FriendRequestDto request = friendService.sendFriendRequest("userA", new SendFriendRequestDto("userB"));
        friendService.declineFriendRequest("userB", request.id());

        FriendRequest updated = friendRequestRepository.findById(request.id()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo("DECLINED");
        assertThat(friendshipRepository.findByUserId("userA")).isEmpty();
        assertThat(friendshipRepository.findByUserId("userB")).isEmpty();

        verify(friendWebSocketController).notifyFriendRequestDeclined(eq("userA"), any());
    }

    // ─── unfriend ───────────────────────────────────────────────────────────

    @Test
    void unfriend_removesBothDirections() {
        userRepository.saveAll(List.of(
            buildUser("userA", "Alice"),
            buildUser("userB", "Bob")
        ));

        FriendRequestDto request = friendService.sendFriendRequest("userA", new SendFriendRequestDto("userB"));
        friendService.acceptFriendRequest("userB", request.id());

        friendService.unfriend("userA", "userB");

        assertThat(friendshipRepository.findByUserId("userA")).isEmpty();
        assertThat(friendshipRepository.findByUserId("userB")).isEmpty();
    }
}
