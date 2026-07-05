package com.aurachat.module.moderation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record WarnUserRequest(
    @NotBlank @Size(max = 500) String message
) {}
