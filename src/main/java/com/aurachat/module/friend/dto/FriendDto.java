package com.aurachat.module.friend.dto;

import java.time.Instant;

public record FriendDto(
    String id,
    String displayName,
    String email,
    String avatarUrl,
    Instant since
) {}
