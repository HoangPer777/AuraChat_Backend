package com.aurachat.module.moderation.dto;

import com.aurachat.module.auth.entity.User;
import com.aurachat.module.moderation.entity.ModerationFlag;

import java.time.Instant;
import java.util.List;

public record ModerationFlagDto(
    String id,
    String contentType,
    String contentId,
    String authorId,
    String authorDisplayName,
    String authorEmail,
    String preview,
    List<String> matchedKeywords,
    String reason,
    String status,
    String reviewedBy,
    Instant reviewedAt,
    String adminNote,
    Instant createdAt
) {
    public static ModerationFlagDto from(ModerationFlag flag, User author) {
        return new ModerationFlagDto(
            flag.getId(),
            flag.getContentType(),
            flag.getContentId(),
            flag.getAuthorId(),
            author == null ? null : author.getDisplayName(),
            author == null ? null : author.getEmail(),
            flag.getPreview(),
            flag.getMatchedKeywords(),
            flag.getReason(),
            flag.getStatus(),
            flag.getReviewedBy(),
            flag.getReviewedAt(),
            flag.getAdminNote(),
            flag.getCreatedAt()
        );
    }
}
