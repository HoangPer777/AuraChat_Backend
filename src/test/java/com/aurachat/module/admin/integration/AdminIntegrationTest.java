package com.aurachat.module.admin.integration;

import com.aurachat.module.auth.entity.User;
import com.aurachat.module.auth.repository.BannedIpRepository;
import com.aurachat.module.auth.repository.UserRepository;
import com.redis.testcontainers.RedisContainer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class AdminIntegrationTest {

    @Container
    static MongoDBContainer mongo = new MongoDBContainer("mongo:7.0");
    @Container
    static RedisContainer redis = new RedisContainer("redis:7.0");

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", mongo::getReplicaSetUrl);
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
        registry.add("imagekit.url-endpoint", () -> "https://ik.imagekit.io/test");
        registry.add("imagekit.public-key", () -> "test-public-key");
        registry.add("imagekit.private-key", () -> "test-private-key");
        registry.add("jwt.secret", () -> "test-jwt-secret-32-bytes-minimum-1234567890");
    }

    @Autowired MockMvc mockMvc;
    @Autowired UserRepository userRepository;
    @Autowired BannedIpRepository bannedIpRepository;

    @AfterEach
    void cleanup() {
        bannedIpRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void adminCanListAndDeactivateUsers() throws Exception {
        User target = userRepository.save(User.builder().email("target@example.com")
            .displayName("Target").role("USER").status("ACTIVE")
            .createdAt(Instant.now()).updatedAt(Instant.now()).build());

        mockMvc.perform(get("/api/admin/users").with(user("admin").roles("ADMIN")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.totalElements").value(1));

        mockMvc.perform(post("/api/admin/users/{id}/deactivate", target.getId())
                .with(user("admin").roles("ADMIN")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("DEACTIVATED"));
    }

    @Test
    void regularUserCannotAccessAdminEndpoints() throws Exception {
        mockMvc.perform(get("/api/admin/users").with(user("member").roles("USER")))
            .andExpect(status().isForbidden());
    }

    @Test
    void adminCanBanAndListIp() throws Exception {
        mockMvc.perform(post("/api/admin/ban-ip")
                .with(user("admin").roles("ADMIN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"ipAddress\":\"10.0.0.9\",\"reason\":\"abuse\"}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.data.ipAddress").value("10.0.0.9"));

        mockMvc.perform(get("/api/admin/banned-ips").with(user("admin").roles("ADMIN")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.totalElements").value(1));
    }
}
