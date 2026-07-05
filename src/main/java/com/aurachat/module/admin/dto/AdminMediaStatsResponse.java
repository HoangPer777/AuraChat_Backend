package com.aurachat.module.admin.dto;

public record AdminMediaStatsResponse(
    long totalCount,
    long activeCount,
    long deletedCount,
    long totalBytes,
    long imageCount,
    long fileCount,
    long audioCount
) {}
