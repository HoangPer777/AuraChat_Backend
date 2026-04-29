package com.aurachat.module.auth.controller;

import com.aurachat.common.response.DataResponse;
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
    public ResponseEntity<DataResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest req) {
        AuthResponse authResponse = authService.register(req);
        return ResponseEntity.ok(DataResponse.success(authResponse, "User registered successfully"));
    }

    @PostMapping("/login")
    public ResponseEntity<DataResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest req) {
        AuthResponse authResponse = authService.login(req);
        return ResponseEntity.ok(DataResponse.success(authResponse, "Login successful"));
    }

    @PostMapping("/refresh")
    public ResponseEntity<DataResponse<AuthResponse>> refresh(@Valid @RequestBody RefreshTokenRequest req) {
        AuthResponse authResponse = authService.refresh(req.refreshToken());
        return ResponseEntity.ok(DataResponse.success(authResponse, "Token refreshed successfully"));
    }

    @GetMapping("/me")
    public ResponseEntity<DataResponse<AuthResponse.UserInfo>> me(@AuthenticationPrincipal String userId) {
        AuthResponse.UserInfo userInfo = authService.me(userId);
        return ResponseEntity.ok(DataResponse.success(userInfo, "User profile retrieved successfully"));
    }

    @PatchMapping("/me")
    public ResponseEntity<DataResponse<AuthResponse.UserInfo>> updateProfile(
        @AuthenticationPrincipal String userId,
        @Valid @RequestBody UpdateProfileRequest req
    ) {
        AuthResponse.UserInfo userInfo = authService.updateProfile(userId, req);
        return ResponseEntity.ok(DataResponse.success(userInfo, "Profile updated successfully"));
    }

    @PostMapping("/logout")
    public ResponseEntity<DataResponse<Void>> logout(@AuthenticationPrincipal String userId) {
        authService.logout(userId);
        return ResponseEntity.ok(DataResponse.success("Logged out successfully"));
    }

    // ─── Forgot / Reset Password ──────────────────────────────────────────────

    @PostMapping("/forgot-password")
    public ResponseEntity<DataResponse<Void>> forgotPassword(
        @Valid @RequestBody ForgotPasswordRequest req
    ) {
        forgotPasswordService.sendOtp(req.email());
        return ResponseEntity.ok(DataResponse.success("If that email exists, an OTP has been sent"));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<DataResponse<Void>> resetPassword(
        @Valid @RequestBody ResetPasswordRequest req
    ) {
        forgotPasswordService.resetPassword(req);
        return ResponseEntity.ok(DataResponse.success("Password reset successfully"));
    }
}
