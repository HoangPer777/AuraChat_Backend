package com.aurachat.module.call.dto;

import jakarta.validation.constraints.NotBlank;

public record GroupCallJoinRequest(
    @NotBlank String callId
) {}
