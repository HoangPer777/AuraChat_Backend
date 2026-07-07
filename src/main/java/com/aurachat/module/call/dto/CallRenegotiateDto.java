package com.aurachat.module.call.dto;

import jakarta.validation.constraints.NotBlank;

public record CallRenegotiateDto(
    @NotBlank String callId,
    @NotBlank String targetUserId,
    @NotBlank String sdp,
    @NotBlank String sdpType
) {}
