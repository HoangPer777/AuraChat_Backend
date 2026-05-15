package com.aurachat.module.media.dto;

public record MediaResponse(
    String url,
    String fileName,
    String originalFileName,
    String contentType,
    long size,
    String provider
) {}
