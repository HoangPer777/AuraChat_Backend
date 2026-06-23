package com.aurachat.module.call.dto;

import jakarta.validation.constraints.NotBlank;

public record GroupCallPeerAnswerDto(
    @NotBlank String callId,
    @NotBlank String targetUserId,
    @NotBlank String sdp
) {
    public String signalType() {
        return "GROUP_PEER_ANSWER";
    }
}
