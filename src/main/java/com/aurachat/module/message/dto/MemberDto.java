package com.aurachat.module.message.dto;

import java.time.Instant;

public record MemberDto(String userId, String role, Instant joinedAt, String displayName, String avatarUrl) {}
