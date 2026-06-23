package com.aurachat.module.call.dto;

import java.util.List;

public record GroupCallParticipantJoinedDto(
    String callId,
    GroupCallParticipantDto participant,
    List<String> joinedParticipantIds
) {
    public String signalType() {
        return "GROUP_PARTICIPANT_JOINED";
    }
}
