package com.aurachat.module.auth.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request DTO for Firebase authentication login.
 * Contains Firebase ID token from client-side authentication.
 */
public record FirebaseLoginRequest(
    @NotBlank(message = "Firebase ID token is required")
    String idToken
) {
}