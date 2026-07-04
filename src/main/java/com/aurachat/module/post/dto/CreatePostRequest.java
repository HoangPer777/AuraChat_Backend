package com.aurachat.module.post.dto;

import jakarta.validation.constraints.Size;

import java.util.List;

public record CreatePostRequest(
    @Size(max = 5000) String content,
    List<@Size(max = 2048) String> imageUrls
) {}
