package com.aurachat.module.moderation.dto;

public record ModerationFlagStatsResponse(
    long pendingTotal,
    long pendingPosts,
    long pendingComments,
    long pendingMedia
) {}
