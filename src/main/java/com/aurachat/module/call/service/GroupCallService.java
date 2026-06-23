package com.aurachat.module.call.service;

import com.aurachat.common.exception.AuthorizationException;
import com.aurachat.common.exception.BusinessLogicException;
import com.aurachat.common.exception.ErrorCode;
import com.aurachat.common.exception.ValidationException;
import com.aurachat.module.auth.repository.UserRepository;
import com.aurachat.module.call.dto.CallResponse;
import com.aurachat.module.call.dto.GroupCallInviteDto;
import com.aurachat.module.call.dto.GroupCallJoinAckDto;
import com.aurachat.module.call.dto.GroupCallParticipantDto;
import com.aurachat.module.call.dto.GroupCallParticipantJoinedDto;
import com.aurachat.module.call.dto.GroupCallParticipantLeftDto;
import com.aurachat.module.call.dto.GroupCallPeerAnswerDto;
import com.aurachat.module.call.dto.GroupCallPeerOfferDto;
import com.aurachat.module.call.dto.GroupCallStartedDto;
import com.aurachat.module.call.dto.IceCandidateDto;
import com.aurachat.module.call.dto.InitiateGroupCallRequest;
import com.aurachat.module.call.entity.CallLog;
import com.aurachat.module.call.repository.CallLogRepository;
import com.aurachat.module.message.entity.Conversation;
import com.aurachat.module.message.service.ConversationService;
import com.aurachat.module.message.service.MessageService;
import com.aurachat.module.message.util.CallLogContent;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class GroupCallService {

    private static final String GROUP_CALL_KEY_PREFIX = "group_call:";
    private static final Duration CALL_TTL = Duration.ofHours(1);

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper redisObjectMapper;
    private final ConversationService conversationService;
    private final UserRepository userRepository;
    private final CallLogRepository callLogRepository;
    private final MessageService messageService;
    private final SimpMessagingTemplate messagingTemplate;

    public GroupCallStartedDto initiateGroupCall(String callerId, InitiateGroupCallRequest request) {
        validateInitiateRequest(request);

        Conversation conversation = conversationService.findAndValidateMember(request.conversationId(), callerId);
        if (!"GROUP".equals(conversation.getType())) {
            throw new BusinessLogicException(ErrorCode.VALIDATION_FAILED,
                "Group calls are only supported for GROUP conversations");
        }

        List<String> allMemberIds = conversation.getMembers() == null ? List.of() :
            conversation.getMembers().stream().map(Conversation.Member::getUserId).toList();

        List<String> inviteIds = allMemberIds.stream()
            .filter(id -> !id.equals(callerId))
            .toList();

        if (inviteIds.isEmpty()) {
            throw new BusinessLogicException(ErrorCode.VALIDATION_FAILED,
                "Group call requires at least one other member");
        }

        String callId = UUID.randomUUID().toString();
        Instant now = Instant.now();

        GroupCallState state = new GroupCallState(
            callId,
            callerId,
            request.conversationId(),
            conversation.getName(),
            request.type(),
            "ACTIVE",
            new ArrayList<>(allMemberIds),
            new ArrayList<>(List.of(callerId)),
            now,
            now,
            null
        );

        saveState(state);

        GroupCallInviteDto invite = new GroupCallInviteDto(
            callId,
            callerId,
            request.conversationId(),
            conversation.getName(),
            request.type(),
            allMemberIds,
            List.of(callerId),
            now
        );

        for (String memberId : inviteIds) {
            messagingTemplate.convertAndSendToUser(memberId, "/queue/call", invite);
        }

        GroupCallStartedDto started = new GroupCallStartedDto(
            callId,
            request.conversationId(),
            conversation.getName(),
            request.type(),
            List.of(callerId),
            now
        );

        messagingTemplate.convertAndSendToUser(callerId, "/queue/call", started);
        return started;
    }

    public void joinGroupCall(String callId, String userId) {
        GroupCallState state = getRequiredState(callId);
        ensureInvited(state, userId);

        if (state.getJoinedParticipantIds().contains(userId)) {
            sendJoinAck(state, userId);
            return;
        }

        state.getJoinedParticipantIds().add(userId);
        if (state.getStartedAt() == null) {
            state.setStartedAt(Instant.now());
        }
        saveState(state);

        GroupCallParticipantDto participant = toParticipantDto(userId);

        GroupCallParticipantJoinedDto joinedEvent = new GroupCallParticipantJoinedDto(
            callId,
            participant,
            List.copyOf(state.getJoinedParticipantIds())
        );

        for (String participantId : state.getJoinedParticipantIds()) {
            if (!participantId.equals(userId)) {
                messagingTemplate.convertAndSendToUser(participantId, "/queue/call", joinedEvent);
            }
        }

        sendJoinAck(state, userId);
    }

    public void relayPeerOffer(String senderId, GroupCallPeerOfferDto offer) {
        GroupCallState state = getRequiredState(offer.callId());
        ensureJoined(state, senderId);
        ensureJoined(state, offer.targetUserId());

        messagingTemplate.convertAndSendToUser(offer.targetUserId(), "/queue/call", Map.of(
            "signalType", "GROUP_PEER_OFFER",
            "callId", offer.callId(),
            "senderId", senderId,
            "targetUserId", offer.targetUserId(),
            "sdp", offer.sdp()
        ));
    }

    public void relayPeerAnswer(String senderId, GroupCallPeerAnswerDto answer) {
        GroupCallState state = getRequiredState(answer.callId());
        ensureJoined(state, senderId);
        ensureJoined(state, answer.targetUserId());

        messagingTemplate.convertAndSendToUser(answer.targetUserId(), "/queue/call", Map.of(
            "signalType", "GROUP_PEER_ANSWER",
            "callId", answer.callId(),
            "senderId", senderId,
            "targetUserId", answer.targetUserId(),
            "sdp", answer.sdp()
        ));
    }

    public boolean tryRelayIceCandidate(String senderId, IceCandidateDto candidate) {
        if (candidate == null || candidate.callId() == null || candidate.callId().isBlank()) {
            return false;
        }

        GroupCallState state = getStateIfPresent(candidate.callId());
        if (state == null) {
            return false;
        }

        if (candidate.receiverId() == null || candidate.receiverId().isBlank()) {
            throw new ValidationException(ErrorCode.VALIDATION_REQUIRED_FIELD,
                "receiverId", null, "Receiver id is required for group ICE candidates");
        }

        ensureJoined(state, senderId);
        ensureJoined(state, candidate.receiverId());

        IceCandidateDto payload = new IceCandidateDto(
            candidate.callId(),
            senderId,
            candidate.receiverId(),
            candidate.candidate(),
            candidate.sdpMid(),
            candidate.sdpMLineIndex()
        );

        messagingTemplate.convertAndSendToUser(candidate.receiverId(), "/queue/call", payload);
        return true;
    }

    public void declineGroupCall(String callId, String userId) {
        GroupCallState state = getStateIfPresent(callId);
        if (state == null) {
            return;
        }
        ensureInvited(state, userId);
    }

    public void leaveGroupCall(String callId, String userId) {
        GroupCallState state = getStateIfPresent(callId);
        if (state == null) {
            return;
        }

        if (!state.getJoinedParticipantIds().contains(userId)) {
            return;
        }

        if (state.getHostId().equals(userId)) {
            endGroupCallInternal(state, "COMPLETED");
            return;
        }

        state.getJoinedParticipantIds().remove(userId);
        saveState(state);

        GroupCallParticipantLeftDto leftEvent = new GroupCallParticipantLeftDto(callId, userId);
        notifyJoinedParticipants(state, leftEvent, userId);
    }

    public void endGroupCall(String callId, String userId) {
        GroupCallState state = getRequiredState(callId);
        ensureJoined(state, userId);
        endGroupCallInternal(state, "COMPLETED");
    }

    private void endGroupCallInternal(GroupCallState state, String status) {
        Instant endedAt = Instant.now();
        long durationSeconds = 0L;
        if (state.getStartedAt() != null) {
            durationSeconds = Duration.between(state.getStartedAt(), endedAt).getSeconds();
        }

        saveCallLog(state, status, endedAt, durationSeconds);
        deleteState(state.getCallId());

        CallResponse response = new CallResponse(
            state.getCallId(),
            status,
            "Group call ended",
            durationSeconds,
            endedAt
        );

        notifyAllInvited(state, Map.of(
            "signalType", "GROUP_END",
            "callId", state.getCallId(),
            "status", status,
            "message", "Group call ended",
            "durationSeconds", durationSeconds,
            "endedAt", endedAt.toString()
        ));
    }

    private void sendJoinAck(GroupCallState state, String userId) {
        List<GroupCallParticipantDto> participants = state.getJoinedParticipantIds().stream()
            .filter(id -> !id.equals(userId))
            .map(this::toParticipantDto)
            .toList();

        GroupCallJoinAckDto ack = new GroupCallJoinAckDto(
            state.getCallId(),
            state.getConversationId(),
            state.getGroupName(),
            state.getType(),
            participants
        );

        messagingTemplate.convertAndSendToUser(userId, "/queue/call", ack);
    }

    private GroupCallParticipantDto toParticipantDto(String userId) {
        return userRepository.findById(userId)
            .map(user -> new GroupCallParticipantDto(
                user.getId(),
                user.getDisplayName(),
                user.getAvatarUrl()
            ))
            .orElse(new GroupCallParticipantDto(userId, "User " + userId.substring(Math.max(0, userId.length() - 6)), null));
    }

    private void notifyJoinedParticipants(GroupCallState state, Object payload, String excludeUserId) {
        for (String participantId : state.getJoinedParticipantIds()) {
            if (!participantId.equals(excludeUserId)) {
                messagingTemplate.convertAndSendToUser(participantId, "/queue/call", payload);
            }
        }
    }

    private void notifyAllInvited(GroupCallState state, Object payload) {
        Set<String> recipients = new LinkedHashSet<>(state.getInvitedParticipantIds());
        recipients.add(state.getHostId());
        for (String userId : recipients) {
            messagingTemplate.convertAndSendToUser(userId, "/queue/call", payload);
        }
    }

    private void saveCallLog(GroupCallState state, String status, Instant endedAt, long durationSeconds) {
        int participantCount = state.getJoinedParticipantIds() == null ? 0 : state.getJoinedParticipantIds().size();

        CallLog logEntry = CallLog.builder()
            .conversationId(state.getConversationId())
            .callerId(state.getHostId())
            .receiverId(null)
            .type(state.getType())
            .status(status)
            .startedAt(state.getStartedAt())
            .endedAt(endedAt)
            .durationSeconds(durationSeconds)
            .createdAt(Instant.now())
            .build();

        callLogRepository.save(logEntry);

        try {
            messageService.sendMessage(state.getHostId(), new com.aurachat.module.message.dto.SendMessageRequest(
                state.getConversationId(),
                CallLogContent.toJson(state.getType(), status, durationSeconds, true, participantCount),
                "CALL_LOG",
                null,
                null,
                null
            ));
        } catch (Exception ex) {
            log.warn("Failed to publish group call log message: callId={}, error={}",
                state.getCallId(), ex.getMessage());
        }
    }

    private void validateInitiateRequest(InitiateGroupCallRequest request) {
        if (request == null) {
            throw new ValidationException(ErrorCode.VALIDATION_REQUIRED_FIELD, "request", null, "Request is required");
        }
        if (request.conversationId() == null || request.conversationId().isBlank()) {
            throw new ValidationException(ErrorCode.VALIDATION_REQUIRED_FIELD,
                "conversationId", request.conversationId(), "Conversation id is required");
        }
        if (request.type() == null || (!"VIDEO".equals(request.type()) && !"AUDIO".equals(request.type()))) {
            throw new ValidationException(ErrorCode.VALIDATION_INVALID_FORMAT,
                "type", request.type(), "Type must be VIDEO or AUDIO");
        }
    }

    private GroupCallState getRequiredState(String callId) {
        GroupCallState state = getStateIfPresent(callId);
        if (state == null) {
            throw new BusinessLogicException(ErrorCode.CALL_NOT_FOUND, "Group call not found or already ended", callId);
        }
        return state;
    }

    private GroupCallState getStateIfPresent(String callId) {
        String json = redisTemplate.opsForValue().get(GROUP_CALL_KEY_PREFIX + callId);
        if (json == null) {
            return null;
        }
        try {
            return redisObjectMapper.readValue(json, GroupCallState.class);
        } catch (Exception ex) {
            log.warn("Failed to parse group call state: callId={}, error={}", callId, ex.getMessage());
            return null;
        }
    }

    private void saveState(GroupCallState state) {
        try {
            String json = redisObjectMapper.writeValueAsString(state);
            redisTemplate.opsForValue().set(GROUP_CALL_KEY_PREFIX + state.getCallId(), json, CALL_TTL);
        } catch (Exception ex) {
            throw new BusinessLogicException(ErrorCode.SYSTEM_ERROR, "Failed to save group call state", "GROUP_CALL_STATE_SAVE");
        }
    }

    private void deleteState(String callId) {
        redisTemplate.delete(GROUP_CALL_KEY_PREFIX + callId);
    }

    private void ensureInvited(GroupCallState state, String userId) {
        if (!state.getInvitedParticipantIds().contains(userId)) {
            throw new AuthorizationException("group_call/" + state.getCallId(), "MEMBER", userId);
        }
    }

    private void ensureJoined(GroupCallState state, String userId) {
        if (!state.getJoinedParticipantIds().contains(userId)) {
            throw new AuthorizationException("group_call/" + state.getCallId(), "JOINED_PARTICIPANT", userId);
        }
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GroupCallState {
        private String callId;
        private String hostId;
        private String conversationId;
        private String groupName;
        private String type;
        private String status;
        private List<String> invitedParticipantIds;
        private List<String> joinedParticipantIds;
        private Instant createdAt;
        private Instant startedAt;
        private Instant endedAt;
    }
}
