package com.aurachat.module.call.service;

import com.aurachat.common.exception.AuthorizationException;
import com.aurachat.common.exception.BusinessLogicException;
import com.aurachat.common.exception.ErrorCode;
import com.aurachat.common.exception.ValidationException;
import com.aurachat.module.call.dto.CallAnswerDto;
import com.aurachat.module.call.dto.CallLogDto;
import com.aurachat.module.call.dto.CallOfferDto;
import com.aurachat.module.call.dto.CallResponse;
import com.aurachat.module.call.dto.IceCandidateDto;
import com.aurachat.module.call.dto.InitiateCallRequest;
import com.aurachat.module.call.entity.CallLog;
import com.aurachat.module.call.repository.CallLogRepository;
import com.aurachat.module.message.service.MessageService;
import com.aurachat.module.notification.service.PushNotificationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PreDestroy;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class CallService {

    private static final String CALL_KEY_PREFIX = "call:";
    private static final Duration CALL_TTL = Duration.ofHours(1);
    private static final Duration CALL_TIMEOUT = Duration.ofSeconds(60);

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper redisObjectMapper;
    private final CallLogRepository callLogRepository;
    private final MessageService messageService;
    private final SimpMessagingTemplate messagingTemplate;
    private final PushNotificationService pushNotificationService;

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread thread = new Thread(r, "call-timeout");
        thread.setDaemon(true);
        return thread;
    });

    public CallResponse initiateCall(String callerId, InitiateCallRequest request) {
        validateInitiateRequest(request);

        String callId = UUID.randomUUID().toString();
        Instant now = Instant.now();

        CallState state = new CallState(
            callId,
            callerId,
            request.receiverId(),
            request.type(),
            "RINGING",
            request.conversationId(),
            now,
            null,
            null
        );

        saveState(state);
        scheduleTimeout(callId);

        CallOfferDto offer = new CallOfferDto(
            callId,
            callerId,
            request.receiverId(),
            request.type(),
            request.sdp(),
            request.conversationId(),
            now
        );

        messagingTemplate.convertAndSendToUser(request.receiverId(), "/queue/call", offer);
        pushNotificationService.notifyIncomingCall(request.receiverId(), offer);

        CallResponse callerAck = new CallResponse(callId, "RINGING", "Call initiated", null, null);
        messagingTemplate.convertAndSendToUser(callerId, "/queue/call", callerAck);

        return callerAck;
    }

    public void acceptCall(String callId, String receiverId, String sdp) {
        if (callId == null || callId.isBlank()) {
            throw new ValidationException(ErrorCode.VALIDATION_REQUIRED_FIELD, "callId", callId, "Call id is required");
        }
        if (sdp == null || sdp.isBlank()) {
            throw new ValidationException(ErrorCode.VALIDATION_REQUIRED_FIELD, "sdp", sdp, "SDP is required");
        }

        CallState state = getStateIfPresent(callId);
        if (state == null) {
            log.warn("Attempted to accept call that no longer exists: callId={}", callId);
            return;
        }
        ensureReceiver(state, receiverId);

        if (!"RINGING".equals(state.getStatus())) {
            return;
        }

        state.setStatus("ACCEPTED");
        state.setStartedAt(Instant.now());
        saveState(state);

        CallAnswerDto answer = new CallAnswerDto(callId, state.getCallerId(), state.getReceiverId(), sdp);
        messagingTemplate.convertAndSendToUser(state.getCallerId(), "/queue/call", answer);
    }

    public void declineCall(String callId, String receiverId) {
        CallState state = getStateIfPresent(callId);
        if (state == null) {
            throw new BusinessLogicException(ErrorCode.CALL_NOT_FOUND, "Call not found or already ended", callId);
        }
        ensureReceiver(state, receiverId);

        if ("DECLINED".equals(state.getStatus()) || "ENDED".equals(state.getStatus()) || "MISSED".equals(state.getStatus())) {
            return;
        }

        Instant endedAt = Instant.now();
        saveCallLog(state, "DECLINED", endedAt, 0L);
        deleteState(callId);

        CallResponse response = new CallResponse(callId, "DECLINED", "Call declined", 0L, endedAt);
        notifyBoth(state, response);
    }

    public void endCall(String callId, String userId) {
        CallState state = getStateIfPresent(callId);
        if (state == null) {
            throw new BusinessLogicException(ErrorCode.CALL_NOT_FOUND, "Call not found or already ended", callId);
        }
        ensureParticipant(state, userId);

        Instant endedAt = Instant.now();
        String status;
        long durationSeconds = 0L;

        if ("ACCEPTED".equals(state.getStatus()) && state.getStartedAt() != null) {
            status = "COMPLETED";
            durationSeconds = Duration.between(state.getStartedAt(), endedAt).getSeconds();
        } else {
            status = "DECLINED";
        }

        saveCallLog(state, status, endedAt, durationSeconds);
        deleteState(callId);

        CallResponse response = new CallResponse(callId, status, "Call ended", durationSeconds, endedAt);
        notifyBoth(state, response);
    }

    public List<CallLogDto> getCallHistory(String userId, Pageable pageable) {
        return callLogRepository.findByUserId(userId, pageable)
            .stream()
            .map(CallLogDto::from)
            .toList();
    }

    public void relayIceCandidate(String senderId, IceCandidateDto candidate) {
        if (candidate == null || candidate.callId() == null || candidate.callId().isBlank()) {
            throw new ValidationException(ErrorCode.VALIDATION_REQUIRED_FIELD, "callId", null, "Call id is required");
        }

        CallState state = getStateIfPresent(candidate.callId());
        if (state == null) {
            log.debug("Skip ICE relay for ended or unknown call: callId={}", candidate.callId());
            return;
        }
        String targetUserId = resolvePeer(state, senderId);

        IceCandidateDto payload = new IceCandidateDto(
            candidate.callId(),
            senderId,
            targetUserId,
            candidate.candidate(),
            candidate.sdpMid(),
            candidate.sdpMLineIndex()
        );

        messagingTemplate.convertAndSendToUser(targetUserId, "/queue/call", payload);
    }

    @PreDestroy
    public void shutdown() {
        scheduler.shutdownNow();
    }

    private void validateInitiateRequest(InitiateCallRequest request) {
        if (request == null) {
            throw new ValidationException(ErrorCode.VALIDATION_REQUIRED_FIELD, "request", null, "Call payload is required");
        }
        if (request.receiverId() == null || request.receiverId().isBlank()) {
            throw new ValidationException(ErrorCode.VALIDATION_REQUIRED_FIELD, "receiverId", request.receiverId(), "Receiver id is required");
        }
        if (request.type() == null || request.type().isBlank()) {
            throw new ValidationException(ErrorCode.VALIDATION_REQUIRED_FIELD, "type", request.type(), "Call type is required");
        }
        if (!"VIDEO".equals(request.type()) && !"AUDIO".equals(request.type())) {
            throw new ValidationException(ErrorCode.VALIDATION_INVALID_FORMAT, "type", request.type(), "Type must be VIDEO or AUDIO");
        }
        if (request.sdp() == null || request.sdp().isBlank()) {
            throw new ValidationException(ErrorCode.VALIDATION_REQUIRED_FIELD, "sdp", request.sdp(), "SDP is required");
        }
    }

    private void scheduleTimeout(String callId) {
        scheduler.schedule(() -> handleTimeout(callId), CALL_TIMEOUT.toSeconds(), TimeUnit.SECONDS);
    }

    private void handleTimeout(String callId) {
        CallState state = getStateIfPresent(callId);
        if (state == null) {
            return;
        }
        if (!"RINGING".equals(state.getStatus())) {
            return;
        }

        Instant endedAt = Instant.now();
        saveCallLog(state, "MISSED", endedAt, 0L);
        deleteState(callId);

        CallResponse response = new CallResponse(callId, "MISSED", "Call missed", 0L, endedAt);
        notifyBoth(state, response);
    }

    private void notifyBoth(CallState state, Object payload) {
        messagingTemplate.convertAndSendToUser(state.getCallerId(), "/queue/call", payload);
        messagingTemplate.convertAndSendToUser(state.getReceiverId(), "/queue/call", payload);
    }

    private void ensureReceiver(CallState state, String receiverId) {
        if (!state.getReceiverId().equals(receiverId)) {
            throw new AuthorizationException("call/" + state.getCallId(), "RECEIVER", receiverId);
        }
    }

    private void ensureParticipant(CallState state, String userId) {
        if (!state.getCallerId().equals(userId) && !state.getReceiverId().equals(userId)) {
            throw new AuthorizationException("call/" + state.getCallId(), "PARTICIPANT", userId);
        }
    }

    private String resolvePeer(CallState state, String senderId) {
        if (state.getCallerId().equals(senderId)) {
            return state.getReceiverId();
        }
        if (state.getReceiverId().equals(senderId)) {
            return state.getCallerId();
        }
        throw new AuthorizationException("call/" + state.getCallId(), "PARTICIPANT", senderId);
    }

    private void saveCallLog(CallState state, String status, Instant endedAt, long durationSeconds) {
        CallLog logEntry = CallLog.builder()
            .conversationId(state.getConversationId())
            .callerId(state.getCallerId())
            .receiverId(state.getReceiverId())
            .type(state.getType())
            .status(status)
            .startedAt(state.getStartedAt())
            .endedAt(endedAt)
            .durationSeconds(durationSeconds)
            .createdAt(Instant.now())
            .build();

        callLogRepository.save(logEntry);
        publishCallLogMessage(state, status, durationSeconds);
    }

    private void publishCallLogMessage(CallState state, String status, long durationSeconds) {
        try {
            messageService.sendCallLogMessage(
                state.getCallerId(),
                state.getReceiverId(),
                state.getConversationId(),
                state.getType(),
                status,
                durationSeconds
            );
        } catch (Exception ex) {
            log.warn("Failed to publish call log chat message: callId={}, error={}",
                state.getCallId(), ex.getMessage());
        }
    }


    private CallState getStateIfPresent(String callId) {
        String key = CALL_KEY_PREFIX + callId;
        String json = redisTemplate.opsForValue().get(key);
        if (json == null) {
            return null;
        }
        try {
            return redisObjectMapper.readValue(json, CallState.class);
        } catch (Exception ex) {
            log.warn("Failed to parse call state: callId={}, error={}", callId, ex.getMessage());
            return null;
        }
    }

    private void saveState(CallState state) {
        String key = CALL_KEY_PREFIX + state.getCallId();
        try {
            String json = redisObjectMapper.writeValueAsString(state);
            redisTemplate.opsForValue().set(key, json, CALL_TTL);
        } catch (Exception ex) {
            throw new BusinessLogicException(ErrorCode.SYSTEM_ERROR, "Failed to save call state", "CALL_STATE_SAVE");
        }
    }

    private void deleteState(String callId) {
        redisTemplate.delete(CALL_KEY_PREFIX + callId);
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CallState {
        private String callId;
        private String callerId;
        private String receiverId;
        private String type;
        private String status;
        private String conversationId;
        private Instant createdAt;
        private Instant startedAt;
        private Instant endedAt;
    }
}
