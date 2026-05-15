package com.aurachat.module.call.dto;

public record IceCandidateDto(
    String callId,
    String senderId,
    String receiverId,
    String candidate,
    String sdpMid,
    Integer sdpMLineIndex
) {}
