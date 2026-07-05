package com.aurachat.module.post.dto;

import java.time.Instant;
import java.util.List;

public record CommentResponse(
    String id,
    AuthorSummary author,
    String content,
    Instant createdAt,
    String parentCommentId,
    AuthorSummary replyTo,
    List<CommentResponse> replies,
    boolean postAuthor
) {}
