package com.aurachat.module.call.integration;

import com.aurachat.common.response.DataResponse;
import com.aurachat.module.call.dto.CallLogDto;
import com.aurachat.module.call.entity.CallLog;
import com.aurachat.module.call.repository.CallLogRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.redis.testcontainers.RedisContainer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class CallControllerIntegrationTest {

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
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CallLogRepository callLogRepository;

    private static final String CALLER = "userA";
    private static final String RECEIVER = "userB";

    @AfterEach
    void cleanup() {
        callLogRepository.deleteAll();
    }

    @Test
    void getCallHistory_returnsEmptyListWhenNoLogs() throws Exception {
        String responseBody = mockMvc.perform(
                get("/api/calls/history")
                    .with(SecurityMockMvcRequestPostProcessors.authentication(
                        new UsernamePasswordAuthenticationToken(CALLER, null, List.of())
                    ))
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andReturn()
            .getResponse()
            .getContentAsString();

        DataResponse<List<CallLogDto>> response = objectMapper.readValue(
            responseBody,
            objectMapper.getTypeFactory().constructParametricType(DataResponse.class, List.class)
        );

        assertThat(response.getData()).isEmpty();
    }

    @Test
    void getCallHistory_returnsAllLogsForUser() throws Exception {
        // Create call logs
        CallLog log1 = CallLog.builder()
            .conversationId("conv1")
            .callerId(CALLER)
            .receiverId(RECEIVER)
            .type("VIDEO")
            .status("COMPLETED")
            .startedAt(Instant.now().minusSeconds(60))
            .endedAt(Instant.now())
            .durationSeconds(45L)
            .createdAt(Instant.now())
            .build();

        CallLog log2 = CallLog.builder()
            .conversationId("conv1")
            .callerId(RECEIVER)
            .receiverId(CALLER)
            .type("AUDIO")
            .status("DECLINED")
            .startedAt(null)
            .endedAt(Instant.now().minusSeconds(120))
            .durationSeconds(0L)
            .createdAt(Instant.now().minusSeconds(120))
            .build();

        CallLog log3 = CallLog.builder()
            .conversationId("conv2")
            .callerId(CALLER)
            .receiverId("userC")
            .type("VIDEO")
            .status("MISSED")
            .startedAt(null)
            .endedAt(Instant.now().minusSeconds(180))
            .durationSeconds(0L)
            .createdAt(Instant.now().minusSeconds(180))
            .build();

        callLogRepository.save(log1);
        callLogRepository.save(log2);
        callLogRepository.save(log3);

        // Get history for CALLER (should include log1 and log3, not log2 in first page)
        String responseBody = mockMvc.perform(
                get("/api/calls/history?page=0&size=50")
                    .with(SecurityMockMvcRequestPostProcessors.authentication(
                        new UsernamePasswordAuthenticationToken(CALLER, null, List.of())
                    ))
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andReturn()
            .getResponse()
            .getContentAsString();

        DataResponse<List<CallLogDto>> response = objectMapper.readValue(
            responseBody,
            objectMapper.getTypeFactory().constructParametricType(DataResponse.class, List.class)
        );

        assertThat(response.getData()).hasSize(3); // All involving CALLER
        assertThat(response.getData()).anySatisfy(log -> 
            assertThat(log.callerId()).isEqualTo(CALLER)
        );
    }

    @Test
    void getCallHistory_supportsPagination() throws Exception {
        // Create 5 call logs for CALLER
        for (int i = 0; i < 5; i++) {
            CallLog log = CallLog.builder()
                .conversationId("conv" + i)
                .callerId(CALLER)
                .receiverId(RECEIVER)
                .type("VIDEO")
                .status("COMPLETED")
                .startedAt(Instant.now().minusSeconds(60 - i * 10))
                .endedAt(Instant.now().minusSeconds(i * 10))
                .durationSeconds(30L)
                .createdAt(Instant.now().minusSeconds(i * 100))
                .build();
            callLogRepository.save(log);
        }

        // Page 1: size=2
        String page1Body = mockMvc.perform(
                get("/api/calls/history?page=0&size=2")
                    .with(SecurityMockMvcRequestPostProcessors.authentication(
                        new UsernamePasswordAuthenticationToken(CALLER, null, List.of())
                    ))
            )
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

        DataResponse<List<CallLogDto>> page1 = objectMapper.readValue(
            page1Body,
            objectMapper.getTypeFactory().constructParametricType(DataResponse.class, List.class)
        );

        assertThat(page1.getData()).hasSize(2);

        // Page 2: size=2
        String page2Body = mockMvc.perform(
                get("/api/calls/history?page=1&size=2")
                    .with(SecurityMockMvcRequestPostProcessors.authentication(
                        new UsernamePasswordAuthenticationToken(CALLER, null, List.of())
                    ))
            )
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

        DataResponse<List<CallLogDto>> page2 = objectMapper.readValue(
            page2Body,
            objectMapper.getTypeFactory().constructParametricType(DataResponse.class, List.class)
        );

        assertThat(page2.getData()).hasSize(2);

        // Page 3: size=2
        String page3Body = mockMvc.perform(
                get("/api/calls/history?page=2&size=2")
                    .with(SecurityMockMvcRequestPostProcessors.authentication(
                        new UsernamePasswordAuthenticationToken(CALLER, null, List.of())
                    ))
            )
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

        DataResponse<List<CallLogDto>> page3 = objectMapper.readValue(
            page3Body,
            objectMapper.getTypeFactory().constructParametricType(DataResponse.class, List.class)
        );

        assertThat(page3.getData()).hasSize(1);
    }

    @Test
    void getCallHistory_filtersByCallStatus() throws Exception {
        CallLog completed = CallLog.builder()
            .conversationId("conv1")
            .callerId(CALLER)
            .receiverId(RECEIVER)
            .type("VIDEO")
            .status("COMPLETED")
            .durationSeconds(30L)
            .createdAt(Instant.now())
            .build();

        CallLog declined = CallLog.builder()
            .conversationId("conv1")
            .callerId(CALLER)
            .receiverId(RECEIVER)
            .type("AUDIO")
            .status("DECLINED")
            .durationSeconds(0L)
            .createdAt(Instant.now())
            .build();

        callLogRepository.save(completed);
        callLogRepository.save(declined);

        String responseBody = mockMvc.perform(
                get("/api/calls/history?page=0&size=50")
                    .with(SecurityMockMvcRequestPostProcessors.authentication(
                        new UsernamePasswordAuthenticationToken(CALLER, null, List.of())
                    ))
            )
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

        DataResponse<List<CallLogDto>> response = objectMapper.readValue(
            responseBody,
            objectMapper.getTypeFactory().constructParametricType(DataResponse.class, List.class)
        );

        assertThat(response.getData()).hasSize(2);
        assertThat(response.getData()).anySatisfy(log -> assertThat(log.status()).isEqualTo("COMPLETED"));
        assertThat(response.getData()).anySatisfy(log -> assertThat(log.status()).isEqualTo("DECLINED"));
    }

    @Test
    void getCallHistory_includesCallMetadata() throws Exception {
        CallLog log = CallLog.builder()
            .id("calllog1")
            .conversationId("conv1")
            .callerId(CALLER)
            .receiverId(RECEIVER)
            .type("VIDEO")
            .status("COMPLETED")
            .startedAt(Instant.now().minusSeconds(60))
            .endedAt(Instant.now())
            .durationSeconds(45L)
            .createdAt(Instant.now())
            .build();

        callLogRepository.save(log);

        String responseBody = mockMvc.perform(
                get("/api/calls/history?page=0&size=50")
                    .with(SecurityMockMvcRequestPostProcessors.authentication(
                        new UsernamePasswordAuthenticationToken(CALLER, null, List.of())
                    ))
            )
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

        DataResponse<List<CallLogDto>> response = objectMapper.readValue(
            responseBody,
            objectMapper.getTypeFactory().constructParametricType(DataResponse.class, List.class)
        );

        assertThat(response.getData()).hasSize(1);
        CallLogDto dto = response.getData().get(0);
        assertThat(dto.id()).isEqualTo("calllog1");
        assertThat(dto.conversationId()).isEqualTo("conv1");
        assertThat(dto.callerId()).isEqualTo(CALLER);
        assertThat(dto.receiverId()).isEqualTo(RECEIVER);
        assertThat(dto.type()).isEqualTo("VIDEO");
        assertThat(dto.status()).isEqualTo("COMPLETED");
        assertThat(dto.durationSeconds()).isEqualTo(45L);
    }
}
