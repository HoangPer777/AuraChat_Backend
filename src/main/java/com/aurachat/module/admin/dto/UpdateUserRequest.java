package com.aurachat.module.admin.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateUserRequest(
    @Size(min = 1, max = 100) String displayName,
    @Pattern(regexp = "USER|ADMIN", message = "role must be USER or ADMIN") String role,
    @Size(max = 500) String bio
) {}
