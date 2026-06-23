package com.aurachat.module.message.dto;

public record UpdateConversationRequest(
    String name,
    String avatarUrl
) {}
