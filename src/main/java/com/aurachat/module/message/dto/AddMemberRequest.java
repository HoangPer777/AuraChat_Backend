package com.aurachat.module.message.dto;

import jakarta.validation.constraints.NotBlank;

public record AddMemberRequest(@NotBlank String userId) {}
