package com.aurachat.module.call.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record InitiateGroupCallRequest(
    @NotBlank String conversationId,
    @NotNull @Pattern(regexp = "VIDEO|AUDIO") String type
) {}
