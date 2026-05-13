package com.aurachat.module.friend.dto;

import java.time.Instant;

public record FriendRequestDto(
    String id,
    UserSummaryDto sender,
    UserSummaryDto receiver,
    String status,
    Instant createdAt
) {
    public record UserSummaryDto(
        String id,
        String displayName,
        String email,
        String avatarUrl
    ) {}
}
