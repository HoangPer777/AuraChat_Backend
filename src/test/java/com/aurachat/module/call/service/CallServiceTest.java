package com.aurachat.module.call.service;

import com.aurachat.common.exception.AuthorizationException;
import com.aurachat.common.exception.BusinessLogicException;
import com.aurachat.common.exception.ErrorCode;
import com.aurachat.common.exception.ValidationException;
import com.aurachat.module.call.dto.CallLogDto;
import com.aurachat.module.call.dto.InitiateCallRequest;
import com.aurachat.module.call.entity.CallLog;
import com.aurachat.module.call.repository.CallLogRepository;
import com.aurachat.module.message.service.MessageService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CallServiceTest {

    @Mock
    private CallLogRepository callLogRepository;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @Mock
    private org.springframework.data.redis.core.RedisTemplate<String, String> redisTemplate;

    @Mock
    private com.fasterxml.jackson.databind.ObjectMapper redisObjectMapper;

    @Mock
    private MessageService messageService;

    @InjectMocks
    private CallService callService;

    private static final String CALLER = "userA";
    private static final String RECEIVER = "userB";
    private static final String CALL_ID = "call123";
    private static final String CONV_ID = "conv1";

    // ─── InitiateCall ─────────────────────────────────────────────────────────

    @Test
    void initiateCall_throwsWhenReceiverIdMissing() {
        InitiateCallRequest request = new InitiateCallRequest(null, "VIDEO", CONV_ID, "sdp");

        assertThatThrownBy(() -> callService.initiateCall(CALLER, request))
            .isInstanceOf(ValidationException.class);
    }

    @Test
    void initiateCall_throwsWhenTypeIsInvalid() {
        InitiateCallRequest request = new InitiateCallRequest(RECEIVER, "INVALID", CONV_ID, "sdp");

        assertThatThrownBy(() -> callService.initiateCall(CALLER, request))
            .isInstanceOf(ValidationException.class);
    }

    @Test
    void initiateCall_throwsWhenSdpMissing() {
        InitiateCallRequest request = new InitiateCallRequest(RECEIVER, "VIDEO", CONV_ID, null);

        assertThatThrownBy(() -> callService.initiateCall(CALLER, request))
            .isInstanceOf(ValidationException.class);
    }

    @Test
    void initiateCall_throwsWhenRequestNull() {
        assertThatThrownBy(() -> callService.initiateCall(CALLER, null))
            .isInstanceOf(ValidationException.class);
    }

    @Test
    void initiateCall_throwsWhenBlankReceiverId() {
        InitiateCallRequest request = new InitiateCallRequest("", "VIDEO", CONV_ID, "sdp");

        assertThatThrownBy(() -> callService.initiateCall(CALLER, request))
            .isInstanceOf(ValidationException.class);
    }

    @Test
    void initiateCall_throwsWhenBlankSdp() {
        InitiateCallRequest request = new InitiateCallRequest(RECEIVER, "VIDEO", CONV_ID, "  ");

        assertThatThrownBy(() -> callService.initiateCall(CALLER, request))
            .isInstanceOf(ValidationException.class);
    }

    // ─── AcceptCall ───────────────────────────────────────────────────────────

    @Test
    void acceptCall_throwsWhenCallIdMissing() {
        assertThatThrownBy(() -> callService.acceptCall(null, RECEIVER, "sdp"))
            .isInstanceOf(ValidationException.class);
    }

    @Test
    void acceptCall_throwsWhenSdpMissing() {
        assertThatThrownBy(() -> callService.acceptCall(CALL_ID, RECEIVER, null))
            .isInstanceOf(ValidationException.class);
    }

    // ─── DeclineCall ──────────────────────────────────────────────────────────

    @Test
    void declineCall_throwsWhenCallNotFound() {
        org.springframework.data.redis.core.ValueOperations<String, String> valueOps = 
            mock(org.springframework.data.redis.core.ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.get("call:" + CALL_ID)).thenReturn(null);

        assertThatThrownBy(() -> callService.declineCall(CALL_ID, RECEIVER))
            .isInstanceOf(BusinessLogicException.class);
    }

    // ─── EndCall ──────────────────────────────────────────────────────────────

    @Test
    void endCall_throwsWhenCallNotFound() {
        org.springframework.data.redis.core.ValueOperations<String, String> valueOps = 
            mock(org.springframework.data.redis.core.ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.get("call:" + CALL_ID)).thenReturn(null);

        assertThatThrownBy(() -> callService.endCall(CALL_ID, CALLER))
            .isInstanceOf(BusinessLogicException.class);
    }

    // ─── GetCallHistory ───────────────────────────────────────────────────────

    @Test
    void getCallHistory_returnsPaginatedLogs() {
        CallLog log1 = CallLog.builder()
            .id("log1")
            .callerId(CALLER)
            .receiverId(RECEIVER)
            .type("VIDEO")
            .status("COMPLETED")
            .durationSeconds(30L)
            .createdAt(Instant.now())
            .build();

        CallLog log2 = CallLog.builder()
            .id("log2")
            .callerId(RECEIVER)
            .receiverId(CALLER)
            .type("AUDIO")
            .status("DECLINED")
            .durationSeconds(0L)
            .createdAt(Instant.now().minusSeconds(60))
            .build();

        Pageable pageable = PageRequest.of(0, 50);
        when(callLogRepository.findByUserId(CALLER, pageable)).thenReturn(List.of(log1, log2));

        List<CallLogDto> result = callService.getCallHistory(CALLER, pageable);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).id()).isEqualTo("log1");
        assertThat(result.get(1).id()).isEqualTo("log2");
    }

    // ─── RelayIceCandidate ────────────────────────────────────────────────────

    @Test
    void relayIceCandidate_throwsWhenCallIdMissing() {
        com.aurachat.module.call.dto.IceCandidateDto candidate = 
            new com.aurachat.module.call.dto.IceCandidateDto(null, CALLER, RECEIVER, "line", "0", 0);

        assertThatThrownBy(() -> callService.relayIceCandidate(CALLER, candidate))
            .isInstanceOf(ValidationException.class);
    }

    @Test
    void relayIceCandidate_throwsWhenCallIdBlank() {
        com.aurachat.module.call.dto.IceCandidateDto candidate = 
            new com.aurachat.module.call.dto.IceCandidateDto("", CALLER, RECEIVER, "line", "0", 0);

        assertThatThrownBy(() -> callService.relayIceCandidate(CALLER, candidate))
            .isInstanceOf(ValidationException.class);
    }

    @Test
    void relayIceCandidate_throwsWhenCallNotFound() {
        com.aurachat.module.call.dto.IceCandidateDto candidate = 
            new com.aurachat.module.call.dto.IceCandidateDto(CALL_ID, CALLER, RECEIVER, "line", "0", 0);

        org.springframework.data.redis.core.ValueOperations<String, String> valueOps = 
            mock(org.springframework.data.redis.core.ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.get("call:" + CALL_ID)).thenReturn(null);

        assertThatThrownBy(() -> callService.relayIceCandidate(CALLER, candidate))
            .isInstanceOf(BusinessLogicException.class);
    }
}
