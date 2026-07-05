package com.aurachat.module.admin.dto;

import com.aurachat.module.auth.entity.User;
import com.aurachat.module.media.entity.Media;

import java.time.Instant;

public record AdminMediaDto(
    String id,
    String fileId,
    String url,
    String fileName,
    String originalFileName,
    String contentType,
    long size,
    String provider,
    String mediaType,
    boolean deleted,
    Instant createdAt,
    Instant deletedAt,
    String ownerId,
    String ownerDisplayName,
    String ownerEmail
) {
    public static AdminMediaDto from(Media media, User owner) {
        return new AdminMediaDto(
            media.getId(),
            media.getFileId(),
            media.getUrl(),
            media.getFileName(),
            media.getOriginalFileName(),
            media.getContentType(),
            media.getSize(),
            media.getProvider(),
            media.getMediaType(),
            media.isDeleted(),
            media.getCreatedAt(),
            media.getDeletedAt(),
            media.getOwnerId(),
            owner == null ? null : owner.getDisplayName(),
            owner == null ? null : owner.getEmail()
        );
    }
}
