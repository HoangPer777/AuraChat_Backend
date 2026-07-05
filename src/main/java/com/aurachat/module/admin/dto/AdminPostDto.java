package com.aurachat.module.admin.dto;

import com.aurachat.module.auth.entity.User;
import com.aurachat.module.post.entity.Post;

import java.time.Instant;
import java.util.List;

public record AdminPostDto(
    String id,
    String authorId,
    String authorDisplayName,
    String authorEmail,
    String authorAvatarUrl,
    String content,
    List<String> imageUrls,
    String originalPostId,
    boolean deleted,
    Instant createdAt,
    Instant updatedAt,
    long likeCount,
    long commentCount,
    long shareCount
) {
    public static AdminPostDto from(
        Post post,
        User author,
        long likeCount,
        long commentCount,
        long shareCount
    ) {
        return new AdminPostDto(
            post.getId(),
            post.getAuthorId(),
            author == null ? null : author.getDisplayName(),
            author == null ? null : author.getEmail(),
            author == null ? null : author.getAvatarUrl(),
            post.getContent(),
            post.getImageUrls(),
            post.getOriginalPostId(),
            post.isDeleted(),
            post.getCreatedAt(),
            post.getUpdatedAt(),
            likeCount,
            commentCount,
            shareCount
        );
    }
}
