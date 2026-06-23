package com.aurachat.module.call.dto;

public record GroupCallParticipantLeftDto(
    String callId,
    String userId
) {
    public String signalType() {
        return "GROUP_PARTICIPANT_LEFT";
    }
}
