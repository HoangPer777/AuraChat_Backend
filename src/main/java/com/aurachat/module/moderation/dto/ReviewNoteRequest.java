package com.aurachat.module.moderation.dto;

import jakarta.validation.constraints.Size;

public record ReviewNoteRequest(
    @Size(max = 500) String note
) {}
