package com.aurachat.module.moderation.dto;

import com.aurachat.module.moderation.entity.ModerationKeyword;

import java.time.Instant;

public record ModerationKeywordDto(
    String id,
    String word,
    boolean enabled,
    Instant createdAt
) {
    public static ModerationKeywordDto from(ModerationKeyword keyword) {
        return new ModerationKeywordDto(
            keyword.getId(),
            keyword.getWord(),
            keyword.isEnabled(),
            keyword.getCreatedAt()
        );
    }
}
