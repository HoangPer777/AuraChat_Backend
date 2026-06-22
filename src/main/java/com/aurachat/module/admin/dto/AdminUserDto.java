package com.aurachat.module.admin.dto;

import com.aurachat.module.auth.entity.User;
import java.time.Instant;

public record AdminUserDto(
    String id,
    String email,
    String displayName,
    String avatarUrl,
    String bio,
    String provider,
    String role,
    String status,
    Instant lastSeen,
    Instant createdAt,
    Instant updatedAt,
    boolean online
) {
    public static AdminUserDto from(User user, boolean online) {
        return new AdminUserDto(user.getId(), user.getEmail(), user.getDisplayName(),
            user.getAvatarUrl(), user.getBio(), user.getProvider(), user.getRole(), user.getStatus(),
            user.getLastSeen(), user.getCreatedAt(), user.getUpdatedAt(), online);
    }
}
