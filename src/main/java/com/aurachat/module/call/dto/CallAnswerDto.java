package com.aurachat.module.call.dto;

public record CallAnswerDto(
    String callId,
    String callerId,
    String receiverId,
    String sdp
) {}
