package com.aurachat.module.call.dto;

import java.time.Instant;
import java.util.List;

public record GroupCallStartedDto(
    String callId,
    String conversationId,
    String groupName,
    String type,
    List<String> joinedParticipantIds,
    Instant createdAt
) {
    public String signalType() {
        return "GROUP_STARTED";
    }

    public String status() {
        return "RINGING";
    }
}
