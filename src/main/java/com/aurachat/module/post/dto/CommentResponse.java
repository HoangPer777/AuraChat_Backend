package com.aurachat.module.post.dto;

import java.time.Instant;

public record CommentResponse(
    String id,
    AuthorSummary author,
    String content,
    Instant createdAt
) {}
