package com.aurachat.module.auth.dto;

public record RegisterResponse(
    String email,
    String displayName,
    boolean emailVerificationRequired,
    String message
) {}
