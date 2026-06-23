package com.aurachat.module.call.dto;

import jakarta.validation.constraints.NotBlank;

public record GroupCallPeerOfferDto(
    @NotBlank String callId,
    @NotBlank String targetUserId,
    @NotBlank String sdp
) {
    public String signalType() {
        return "GROUP_PEER_OFFER";
    }
}
