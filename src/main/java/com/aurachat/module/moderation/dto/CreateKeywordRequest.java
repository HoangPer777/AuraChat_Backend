package com.aurachat.module.moderation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateKeywordRequest(
    @NotBlank @Size(max = 100) String word
) {}
