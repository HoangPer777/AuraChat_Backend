package com.aurachat.module.call.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record InitiateCallRequest(
    @NotBlank String receiverId,
    @NotNull @Pattern(regexp = "VIDEO|AUDIO") String type,
    String conversationId,
    @NotBlank String sdp
) {}
