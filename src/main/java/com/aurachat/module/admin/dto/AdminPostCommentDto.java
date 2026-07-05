package com.aurachat.module.admin.dto;

import com.aurachat.module.auth.entity.User;
import com.aurachat.module.post.entity.PostComment;

import java.time.Instant;

public record AdminPostCommentDto(
    String id,
    String postId,
    String authorId,
    String authorDisplayName,
    String authorEmail,
    String content,
    String parentCommentId,
    Instant createdAt
) {
    public static AdminPostCommentDto from(PostComment comment, User author) {
        return new AdminPostCommentDto(
            comment.getId(),
            comment.getPostId(),
            comment.getAuthorId(),
            author == null ? null : author.getDisplayName(),
            author == null ? null : author.getEmail(),
            comment.getContent(),
            comment.getParentCommentId(),
            comment.getCreatedAt()
        );
    }
}
