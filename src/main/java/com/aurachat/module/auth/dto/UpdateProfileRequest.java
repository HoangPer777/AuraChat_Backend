package com.aurachat.module.auth.dto;

import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
    @Size(min = 2, max = 100)
    String displayName,

    @Size(max = 500)
    String bio
) {}
