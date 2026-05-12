package com.aurachat.module.message.dto;

import com.aurachat.module.message.entity.Message;

import java.time.Instant;
import java.util.List;

public record MessageResponse(
    String id,
    String conversationId,
    String senderId,
    String type,
    String content,
    String fileUrl,
    String fileName,
    Long fileSize,
    List<SeenEntryDto> seenBy,
    boolean isDeleted,
    Instant createdAt
) {
    public static MessageResponse from(Message msg) {
        List<SeenEntryDto> seen = msg.getSeenBy() == null ? List.of() :
            msg.getSeenBy().stream()
                .map(e -> new SeenEntryDto(e.getUserId(), e.getSeenAt()))
                .toList();
        return new MessageResponse(
            msg.getId(), msg.getConversationId(), msg.getSenderId(),
            msg.getType(), msg.getContent(), msg.getFileUrl(),
            msg.getFileName(), msg.getFileSize(), seen,
            msg.isDeleted(), msg.getCreatedAt()
        );
    }
}
