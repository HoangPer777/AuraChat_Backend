package com.aurachat.module.post.dto;

import java.time.Instant;
import java.util.List;

public record PostSummary(
    String id,
    AuthorSummary author,
    String content,
    List<String> imageUrls,
    Instant createdAt
) {}
