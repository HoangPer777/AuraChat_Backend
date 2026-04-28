package com.aurachat.module.auth.controller;

import com.aurachat.module.auth.service.AuthService;
import com.aurachat.module.auth.service.ForgotPasswordService;
import com.aurachat.module.auth.dto.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final ForgotPasswordService forgotPasswordService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest req) {
        return ResponseEntity.ok(authService.register(req));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest req) {
        return ResponseEntity.ok(authService.login(req));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshTokenRequest req) {
        return ResponseEntity.ok(authService.refresh(req.refreshToken()));
    }

    @GetMapping("/me")
    public ResponseEntity<AuthResponse.UserInfo> me(@AuthenticationPrincipal String userId) {
        return ResponseEntity.ok(authService.me(userId));
    }

    @PatchMapping("/me")
    public ResponseEntity<AuthResponse.UserInfo> updateProfile(
        @AuthenticationPrincipal String userId,
        @Valid @RequestBody UpdateProfileRequest req
    ) {
        return ResponseEntity.ok(authService.updateProfile(userId, req));
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(@AuthenticationPrincipal String userId) {
        authService.logout(userId);
        return ResponseEntity.ok(Map.of("message", "Logged out successfully."));
    }

    // ─── Forgot / Reset Password ──────────────────────────────────────────────

    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, String>> forgotPassword(
        @Valid @RequestBody ForgotPasswordRequest req
    ) {
        forgotPasswordService.sendOtp(req.email());
        return ResponseEntity.ok(Map.of("message", "If that email exists, an OTP has been sent."));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, String>> resetPassword(
        @Valid @RequestBody ResetPasswordRequest req
    ) {
        forgotPasswordService.resetPassword(req);
        return ResponseEntity.ok(Map.of("message", "Password reset successfully."));
    }
}
