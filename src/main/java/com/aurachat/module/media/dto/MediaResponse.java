package com.aurachat.module.media.dto;

import java.time.Instant;

public record MediaResponse(
    String id,
    String fileId,
    String url,
    String fileName,
    String originalFileName,
    String contentType,
    long size,
    String provider,
    String mediaType,
    Instant createdAt
) {}
