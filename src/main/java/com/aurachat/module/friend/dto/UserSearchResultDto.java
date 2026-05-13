package com.aurachat.module.friend.dto;

public record UserSearchResultDto(
    String id,
    String displayName,
    String email,
    String avatarUrl
) {}
