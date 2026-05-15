package com.aurachat.module.call.dto;

import com.aurachat.module.call.entity.CallLog;

import java.time.Instant;

public record CallLogDto(
    String id,
    String conversationId,
    String callerId,
    String receiverId,
    String type,
    String status,
    Instant startedAt,
    Instant endedAt,
    Long durationSeconds,
    Instant createdAt
) {
    public static CallLogDto from(CallLog log) {
        return new CallLogDto(
            log.getId(),
            log.getConversationId(),
            log.getCallerId(),
            log.getReceiverId(),
            log.getType(),
            log.getStatus(),
            log.getStartedAt(),
            log.getEndedAt(),
            log.getDurationSeconds(),
            log.getCreatedAt()
        );
    }
}
