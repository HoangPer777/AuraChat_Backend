package com.aurachat.module.call.dto;

import java.time.Instant;
import java.util.List;

public record GroupCallInviteDto(
    String callId,
    String callerId,
    String conversationId,
    String groupName,
    String type,
    List<String> invitedParticipantIds,
    List<String> joinedParticipantIds,
    Instant createdAt
) {
    public String signalType() {
        return "GROUP_INVITE";
    }
}
