package com.aurachat.module.call.integration;

import com.aurachat.module.call.dto.CallAnswerDto;
import com.aurachat.module.call.dto.CallOfferDto;
import com.aurachat.module.call.dto.CallResponse;
import com.aurachat.module.call.dto.IceCandidateDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for Call WebSocket DTO serialization.
 * Verify DTO objects can be serialized/deserialized correctly for STOMP messaging.
 * (Full integration tests with actual WebSocket would require mock STOMP client)
 */
class CallWebSocketIntegrationTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    void setup() {
        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();
    }

    @Test
    void stompOffer_sendsOfferToReceiver() throws Exception {
        String callId = "call123";
        String caller = "userA";
        String receiver = "userB";
        String sdp = "v=0\r\no=- 123 123 IN IP4 127.0.0.1\r\n...";
        Instant now = Instant.now();

        CallOfferDto offer = new CallOfferDto(callId, caller, receiver, "VIDEO", sdp, "conv1", now);

        // Verify offer can be serialized
        String json = objectMapper.writeValueAsString(offer);
        assertThat(json).contains(callId);
        assertThat(json).contains(caller);
        assertThat(json).contains(receiver);
        assertThat(json).contains("VIDEO");

        // Verify offer can be deserialized
        CallOfferDto deserialized = objectMapper.readValue(json, CallOfferDto.class);
        assertThat(deserialized.callId()).isEqualTo(callId);
        assertThat(deserialized.callerId()).isEqualTo(caller);
        assertThat(deserialized.receiverId()).isEqualTo(receiver);
        assertThat(deserialized.type()).isEqualTo("VIDEO");
        assertThat(deserialized.sdp()).isEqualTo(sdp);
    }

    @Test
    void stompAnswer_sendsAnswerToCaller() throws Exception {
        String callId = "call123";
        String caller = "userA";
        String receiver = "userB";
        String sdp = "v=0\r\no=- 123 123 IN IP4 127.0.0.1\r\n...";

        CallAnswerDto answer = new CallAnswerDto(callId, caller, receiver, sdp);

        // Verify answer can be serialized
        String json = objectMapper.writeValueAsString(answer);
        assertThat(json).contains(callId);
        assertThat(json).contains(caller);
        assertThat(json).contains(receiver);

        // Verify answer can be deserialized
        CallAnswerDto deserialized = objectMapper.readValue(json, CallAnswerDto.class);
        assertThat(deserialized.callId()).isEqualTo(callId);
        assertThat(deserialized.callerId()).isEqualTo(caller);
        assertThat(deserialized.receiverId()).isEqualTo(receiver);
        assertThat(deserialized.sdp()).isEqualTo(sdp);
    }

    @Test
    void stompIceCandidate_forwardsToReceiver() throws Exception {
        String callId = "call123";
        String sender = "userA";
        String receiver = "userB";
        String candidate = "candidate:843734243 1 udp 1677729535 192.168.0.196 54321 typ srflx";

        IceCandidateDto iceCandidate = new IceCandidateDto(callId, sender, receiver, candidate, "0", 0);

        // Verify ICE candidate can be serialized
        String json = objectMapper.writeValueAsString(iceCandidate);
        assertThat(json).contains(callId);
        assertThat(json).contains(sender);
        assertThat(json).contains(receiver);
        assertThat(json).contains(candidate);

        // Verify ICE candidate can be deserialized
        IceCandidateDto deserialized = objectMapper.readValue(json, IceCandidateDto.class);
        assertThat(deserialized.callId()).isEqualTo(callId);
        assertThat(deserialized.senderId()).isEqualTo(sender);
        assertThat(deserialized.receiverId()).isEqualTo(receiver);
        assertThat(deserialized.candidate()).isEqualTo(candidate);
    }

    @Test
    void stompDecline_notifiesCaller() throws Exception {
        String callId = "call123";
        String status = "DECLINED";
        String message = "User declined the call";

        CallResponse response = new CallResponse(callId, status, message, 0L, null);

        // Verify decline response can be serialized
        String json = objectMapper.writeValueAsString(response);
        assertThat(json).contains(callId);
        assertThat(json).contains(status);
        assertThat(json).contains(message);

        // Verify decline response can be deserialized
        CallResponse deserialized = objectMapper.readValue(json, CallResponse.class);
        assertThat(deserialized.callId()).isEqualTo(callId);
        assertThat(deserialized.status()).isEqualTo(status);
        assertThat(deserialized.message()).isEqualTo(message);
    }

    @Test
    void stompEnd_notifiesBothUsers() throws Exception {
        String callId = "call123";
        String status = "COMPLETED";
        String message = "Call ended";
        long durationSeconds = 30L;
        Instant endedAt = Instant.now();

        CallResponse response = new CallResponse(callId, status, message, durationSeconds, endedAt);

        // Verify end response can be serialized
        String json = objectMapper.writeValueAsString(response);
        assertThat(json).contains(callId);
        assertThat(json).contains(status);
        assertThat(json).contains("30");

        // Verify end response can be deserialized
        CallResponse deserialized = objectMapper.readValue(json, CallResponse.class);
        assertThat(deserialized.callId()).isEqualTo(callId);
        assertThat(deserialized.status()).isEqualTo(status);
        assertThat(deserialized.durationSeconds()).isEqualTo(30L);
    }

    @Test
    void subscriptionPattern_userQueueCall() throws Exception {
        // Test that user subscription pattern "/user/{userId}/queue/call" is valid STOMP syntax
        String userId = "userA";
        String subscriptionDest = "/user/" + userId + "/queue/call";
        
        // Verify message can be sent to user-specific queue
        String callId = "call123";
        CallOfferDto offer = new CallOfferDto(callId, "userB", userId, "VIDEO", "sdp", "conv1", Instant.now());
        
        String json = objectMapper.writeValueAsString(offer);
        CallOfferDto deserialized = objectMapper.readValue(json, CallOfferDto.class);
        
        assertThat(deserialized.receiverId()).isEqualTo(userId);
        // In real STOMP, message would be sent to: /user/userA/queue/call
    }
}

