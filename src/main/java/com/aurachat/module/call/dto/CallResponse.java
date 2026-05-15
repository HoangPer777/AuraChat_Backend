package com.aurachat.module.call.dto;

import java.time.Instant;

public record CallResponse(
    String callId,
    String status,
    String message,
    Long durationSeconds,
    Instant endedAt
) {}
