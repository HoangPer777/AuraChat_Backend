package com.aurachat.module.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record BanIpRequest(
    @NotBlank @Size(max = 45) String ipAddress,
    @NotBlank @Size(max = 500) String reason
) {}
