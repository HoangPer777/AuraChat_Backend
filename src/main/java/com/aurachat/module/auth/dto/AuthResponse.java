package com.aurachat.module.auth.dto;

import com.aurachat.module.auth.entity.User;

public record AuthResponse(
    String accessToken,
    String refreshToken,
    String tokenType,
    UserInfo user
) {
    public record UserInfo(
        String id,
        String email,
        String displayName,
        String avatarUrl,
        String bio
    ) {}

    public static AuthResponse of(String accessToken, String refreshToken, User user) {
        return new AuthResponse(
            accessToken,
            refreshToken,
            "Bearer",
            new UserInfo(user.getId(), user.getEmail(), user.getDisplayName(),
                         user.getAvatarUrl(), user.getBio())
        );
    }
}
