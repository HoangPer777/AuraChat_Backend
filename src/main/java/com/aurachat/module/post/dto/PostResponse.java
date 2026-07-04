package com.aurachat.module.post.dto;

import java.time.Instant;
import java.util.List;

public record PostResponse(
    String id,
    AuthorSummary author,
    String content,
    List<String> imageUrls,
    String originalPostId,
    PostSummary originalPost,
    long likeCount,
    long commentCount,
    long shareCount,
    boolean likedByMe,
    Instant createdAt
) {}
