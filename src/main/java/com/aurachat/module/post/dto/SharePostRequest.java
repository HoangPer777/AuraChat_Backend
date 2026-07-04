package com.aurachat.module.post.dto;

import jakarta.validation.constraints.Size;

public record SharePostRequest(
    @Size(max = 5000) String content
) {}
