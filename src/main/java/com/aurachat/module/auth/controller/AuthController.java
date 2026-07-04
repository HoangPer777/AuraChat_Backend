package com.aurachat.module.auth.controller;

import com.aurachat.common.response.DataResponse;
import com.aurachat.module.auth.service.AuthService;
import com.aurachat.module.auth.service.EmailVerificationService;
import com.aurachat.module.auth.service.ForgotPasswordService;
import com.aurachat.module.auth.service.AvatarUploadService;
import com.aurachat.module.auth.service.FcmTokenService;
import com.aurachat.module.auth.service.FirebaseAuthService;
import com.aurachat.module.auth.dto.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final ForgotPasswordService forgotPasswordService;
    private final EmailVerificationService emailVerificationService;
    private final AvatarUploadService avatarUploadService;
    private final FirebaseAuthService firebaseAuthService;
    private final FcmTokenService fcmTokenService;

    @PostMapping("/register")
    public ResponseEntity<DataResponse<RegisterResponse>> register(@Valid @RequestBody RegisterRequest req) {
        RegisterResponse response = authService.register(req);
        return ResponseEntity.ok(DataResponse.success(response, response.message()));
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

    @PatchMapping("/me/password")
    public ResponseEntity<DataResponse<Void>> changePassword(
        @AuthenticationPrincipal String userId,
        @Valid @RequestBody ChangePasswordRequest req
    ) {
        authService.changePassword(userId, req);
        return ResponseEntity.ok(DataResponse.success("Password changed successfully"));
    }

    @PostMapping("/logout")
    public ResponseEntity<DataResponse<Void>> logout(@AuthenticationPrincipal String userId) {
        authService.logout(userId);
        return ResponseEntity.ok(DataResponse.success("Logged out successfully"));
    }

    @PostMapping("/me/avatar")
    public ResponseEntity<DataResponse<String>> uploadAvatar(
        @AuthenticationPrincipal String userId,
        @RequestParam("file") MultipartFile file
    ) {
        String avatarUrl = avatarUploadService.uploadAvatar(userId, file);
        return ResponseEntity.ok(DataResponse.success(avatarUrl, "Avatar uploaded successfully"));
    }

    @PostMapping("/me/fcm-token")
    public ResponseEntity<DataResponse<Void>> registerFcmToken(
        @AuthenticationPrincipal String userId,
        @Valid @RequestBody FcmTokenRequest req
    ) {
        fcmTokenService.registerToken(userId, req.token());
        return ResponseEntity.ok(DataResponse.success("FCM token registered"));
    }

    @DeleteMapping("/me/fcm-token")
    public ResponseEntity<DataResponse<Void>> removeFcmToken(
        @AuthenticationPrincipal String userId,
        @Valid @RequestBody FcmTokenRequest req
    ) {
        fcmTokenService.removeToken(userId, req.token());
        return ResponseEntity.ok(DataResponse.success("FCM token removed"));
    }

    // ─── Firebase Authentication ──────────────────────────────────────────────

    @PostMapping("/firebase/login")
    public ResponseEntity<DataResponse<AuthResponse>> firebaseLogin(@Valid @RequestBody FirebaseLoginRequest req) {
        AuthResponse authResponse = firebaseAuthService.loginWithFirebase(req);
        return ResponseEntity.ok(DataResponse.success(authResponse, "Firebase login successful"));
    }

    // ─── Forgot / Reset Password ──────────────────────────────────────────────

    @PostMapping("/forgot-password")
    public ResponseEntity<DataResponse<ForgotPasswordResponse>> forgotPassword(
        @Valid @RequestBody ForgotPasswordRequest req
    ) {
        ForgotPasswordResponse response = forgotPasswordService.requestResetLink(req.email());
        return ResponseEntity.ok(DataResponse.success(response, response.message()));
    }

    @GetMapping("/verify-forgot-password")
    public ResponseEntity<DataResponse<VerifyForgotPasswordResponse>> verifyForgotPassword(
        @RequestParam String token
    ) {
        String email = forgotPasswordService.verifyResetEmail(token);
        return ResponseEntity.ok(DataResponse.success(
            new VerifyForgotPasswordResponse(email),
            "Email verified for password reset"
        ));
    }

    @GetMapping("/forgot-password/status")
    public ResponseEntity<DataResponse<ForgotPasswordStatusResponse>> forgotPasswordStatus(
        @RequestParam String email
    ) {
        boolean verified = forgotPasswordService.isResetEmailVerified(email);
        return ResponseEntity.ok(DataResponse.success(
            new ForgotPasswordStatusResponse(verified),
            verified ? "Email verified" : "Email not verified yet"
        ));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<DataResponse<Void>> resetPassword(
        @Valid @RequestBody ResetPasswordRequest req
    ) {
        forgotPasswordService.resetPassword(req);
        return ResponseEntity.ok(DataResponse.success("Password reset successfully"));
    }

    // ─── Email Verification (Registration) ────────────────────────────────────

    @GetMapping("/verify-email")
    public ResponseEntity<DataResponse<Void>> verifyEmail(@RequestParam String token) {
        emailVerificationService.verifyRegistrationEmail(token);
        return ResponseEntity.ok(DataResponse.success("Email verified successfully"));
    }

    @PostMapping("/resend-verification")
    public ResponseEntity<DataResponse<Void>> resendVerification(
        @Valid @RequestBody ResendVerificationRequest req
    ) {
        emailVerificationService.resendVerificationEmail(req.email());
        return ResponseEntity.ok(DataResponse.success("Verification email sent"));
    }
}
