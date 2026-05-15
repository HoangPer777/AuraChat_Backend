package com.aurachat.module.call.dto;

import java.time.Instant;

public record CallOfferDto(
    String callId,
    String callerId,
    String receiverId,
    String type,
    String sdp,
    String conversationId,
    Instant createdAt
) {}
