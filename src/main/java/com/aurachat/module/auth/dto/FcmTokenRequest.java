package com.aurachat.module.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record FcmTokenRequest(
    @NotBlank(message = "FCM token is required")
    String token
) {}
