package com.aurachat.module.auth.service;

import com.aurachat.common.exception.AuthenticationException;
import com.aurachat.common.exception.BusinessLogicException;
import com.aurachat.common.exception.ErrorCode;
import com.aurachat.common.exception.ValidationException;
import com.aurachat.config.JwtUtil;
import com.aurachat.module.auth.dto.*;
import com.aurachat.module.auth.entity.RefreshToken;
import com.aurachat.module.auth.entity.User;
import com.aurachat.module.auth.repository.RefreshTokenRepository;
import com.aurachat.module.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final EmailVerificationService emailVerificationService;

    // ─── Register ────────────────────────────────────────────────────────────

    public RegisterResponse register(RegisterRequest req) {
        if (userRepository.existsByEmail(req.email())) {
            throw new BusinessLogicException(
                ErrorCode.USER_EMAIL_EXISTS, 
                "Email uniqueness constraint"
            );
        }

        User user = User.builder()
            .email(req.email())
            .displayName(req.displayName())
            .passwordHash(passwordEncoder.encode(req.password()))
            .emailVerified(false)
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();

        userRepository.save(user);
        emailVerificationService.sendRegistrationVerificationEmail(user);

        return new RegisterResponse(
            user.getEmail(),
            user.getDisplayName(),
            true,
            "Đăng ký thành công. Vui lòng kiểm tra email để xác nhận tài khoản trước khi đăng nhập."
        );
    }

    // ─── Login ────────────────────────────────────────────────────────────────

    public AuthResponse login(LoginRequest req) {
        User user = userRepository.findByEmail(req.email())
            .orElseThrow(() -> new AuthenticationException(
                ErrorCode.AUTH_INVALID_CREDENTIALS,
                "Invalid email or password",
                "login"
            ));

        if (!passwordEncoder.matches(req.password(), user.getPasswordHash())) {
            throw new AuthenticationException(
                ErrorCode.AUTH_INVALID_CREDENTIALS,
                "Invalid email or password",
                "login"
            );
        }

        ensureEmailVerified(user);
        ensureAccountActive(user, "login");

        String accessToken = jwtUtil.generateAccessToken(user.getId());
        String refreshToken = createRefreshToken(user.getId());
        return AuthResponse.of(accessToken, refreshToken, user);
    }

    // ─── Refresh Token ────────────────────────────────────────────────────────

    public AuthResponse refresh(String rawToken) {
        RefreshToken rt = refreshTokenRepository.findByToken(rawToken)
            .orElseThrow(() -> new AuthenticationException(
                ErrorCode.AUTH_TOKEN_INVALID,
                "Invalid refresh token",
                "refresh token"
            ));

        if (rt.isExpired()) {
            refreshTokenRepository.delete(rt);
            throw new AuthenticationException(
                ErrorCode.AUTH_TOKEN_EXPIRED,
                "Refresh token expired",
                "refresh token"
            );
        }

        User user = userRepository.findById(rt.getUserId())
            .orElseThrow(() -> new BusinessLogicException(
                ErrorCode.USER_NOT_FOUND,
                "User associated with refresh token not found"
            ));

        ensureAccountActive(user, "refresh token");

        // Rotate refresh token
        refreshTokenRepository.delete(rt);
        String newAccessToken = jwtUtil.generateAccessToken(user.getId());
        String newRefreshToken = createRefreshToken(user.getId());
        return AuthResponse.of(newAccessToken, newRefreshToken, user);
    }

    // ─── Get current user ─────────────────────────────────────────────────────

    public AuthResponse.UserInfo me(String userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new BusinessLogicException(
                ErrorCode.USER_NOT_FOUND,
                "User not found"
            ));
        return new AuthResponse.UserInfo(
            user.getId(), user.getEmail(), user.getDisplayName(),
            user.getAvatarUrl(), user.getBio(), user.getRole(), user.getStatus(),
            user.getProvider()
        );
    }

    // ─── Update profile ───────────────────────────────────────────────────────

    public AuthResponse.UserInfo updateProfile(String userId, UpdateProfileRequest req) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new BusinessLogicException(
                ErrorCode.USER_NOT_FOUND,
                "User not found"
            ));

        if (req.displayName() != null) user.setDisplayName(req.displayName());
        if (req.bio() != null) user.setBio(req.bio());
        user.setUpdatedAt(Instant.now());
        userRepository.save(user);

        return new AuthResponse.UserInfo(
            user.getId(), user.getEmail(), user.getDisplayName(),
            user.getAvatarUrl(), user.getBio(), user.getRole(), user.getStatus(),
            user.getProvider()
        );
    }

    // ─── Change password ──────────────────────────────────────────────────────

    public void changePassword(String userId, ChangePasswordRequest req) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new BusinessLogicException(
                ErrorCode.USER_NOT_FOUND,
                "User not found"
            ));

        if (!"LOCAL".equalsIgnoreCase(user.getProvider()) || user.getPasswordHash() == null) {
            throw new BusinessLogicException(
                ErrorCode.VALIDATION_FAILED,
                "Password change is not available for this account"
            );
        }

        if (!passwordEncoder.matches(req.currentPassword(), user.getPasswordHash())) {
            throw new AuthenticationException(
                ErrorCode.AUTH_INVALID_CREDENTIALS,
                "Current password is incorrect",
                "change password"
            );
        }

        if (req.currentPassword().equals(req.newPassword())) {
            throw new ValidationException(
                ErrorCode.VALIDATION_INVALID_FORMAT,
                "newPassword",
                req.newPassword(),
                "New password must be different from current password"
            );
        }

        user.setPasswordHash(passwordEncoder.encode(req.newPassword()));
        user.setUpdatedAt(Instant.now());
        userRepository.save(user);
    }

    // ─── Update avatar URL (called by Media_Service flow) ────────────────────

    public void updateAvatarUrl(String userId, String avatarUrl) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new BusinessLogicException(
                ErrorCode.USER_NOT_FOUND,
                "User not found"
            ));
        user.setAvatarUrl(avatarUrl);
        user.setUpdatedAt(Instant.now());
        userRepository.save(user);
    }

    // ─── Logout ───────────────────────────────────────────────────────────────

    public void logout(String userId) {
        refreshTokenRepository.deleteByUserId(userId);
    }

    // ─── Helper ───────────────────────────────────────────────────────────────

    private String createRefreshToken(String userId) {
        RefreshToken rt = RefreshToken.builder()
            .token(UUID.randomUUID().toString())
            .userId(userId)
            .expiresAt(Instant.now().plus(7, ChronoUnit.DAYS))
            .createdAt(Instant.now())
            .build();
        refreshTokenRepository.save(rt);
        return rt.getToken();
    }

    private void ensureAccountActive(User user, String attemptedAction) {
        if (!"ACTIVE".equalsIgnoreCase(user.getStatus())) {
            throw new AuthenticationException(
                ErrorCode.AUTH_ACCOUNT_LOCKED,
                "Account status is " + user.getStatus(),
                attemptedAction
            );
        }
    }

    private void ensureEmailVerified(User user) {
        if (EmailVerificationService.isLocalAccount(user) && Boolean.FALSE.equals(user.getEmailVerified())) {
            throw new AuthenticationException(
                ErrorCode.AUTH_EMAIL_NOT_VERIFIED,
                "Email address is not verified",
                "login"
            );
        }
    }
}
