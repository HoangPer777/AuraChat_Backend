package com.aurachat.module.moderation.dto;

import jakarta.validation.constraints.Size;

public record FlagMediaRequest(
    @Size(max = 500) String note
) {}
