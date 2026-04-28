package com.aurachat.module.auth.service;

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

    // ─── Register ────────────────────────────────────────────────────────────

    public AuthResponse register(RegisterRequest req) {
        if (userRepository.existsByEmail(req.email())) {
            throw new IllegalArgumentException("Email already in use");
        }

        User user = User.builder()
            .email(req.email())
            .displayName(req.displayName())
            .passwordHash(passwordEncoder.encode(req.password()))
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();

        userRepository.save(user);

        String accessToken = jwtUtil.generateAccessToken(user.getId());
        String refreshToken = createRefreshToken(user.getId());
        return AuthResponse.of(accessToken, refreshToken, user);
    }

    // ─── Login ────────────────────────────────────────────────────────────────

    public AuthResponse login(LoginRequest req) {
        User user = userRepository.findByEmail(req.email())
            .orElseThrow(() -> new IllegalArgumentException("Invalid email or password"));

        if (!passwordEncoder.matches(req.password(), user.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid email or password");
        }

        String accessToken = jwtUtil.generateAccessToken(user.getId());
        String refreshToken = createRefreshToken(user.getId());
        return AuthResponse.of(accessToken, refreshToken, user);
    }

    // ─── Refresh Token ────────────────────────────────────────────────────────

    public AuthResponse refresh(String rawToken) {
        RefreshToken rt = refreshTokenRepository.findByToken(rawToken)
            .orElseThrow(() -> new IllegalArgumentException("Invalid refresh token"));

        if (rt.isExpired()) {
            refreshTokenRepository.delete(rt);
            throw new IllegalArgumentException("Refresh token expired");
        }

        User user = userRepository.findById(rt.getUserId())
            .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // Rotate refresh token
        refreshTokenRepository.delete(rt);
        String newAccessToken = jwtUtil.generateAccessToken(user.getId());
        String newRefreshToken = createRefreshToken(user.getId());
        return AuthResponse.of(newAccessToken, newRefreshToken, user);
    }

    // ─── Get current user ─────────────────────────────────────────────────────

    public AuthResponse.UserInfo me(String userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));
        return new AuthResponse.UserInfo(
            user.getId(), user.getEmail(), user.getDisplayName(),
            user.getAvatarUrl(), user.getBio()
        );
    }

    // ─── Update profile ───────────────────────────────────────────────────────

    public AuthResponse.UserInfo updateProfile(String userId, UpdateProfileRequest req) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (req.displayName() != null) user.setDisplayName(req.displayName());
        if (req.bio() != null) user.setBio(req.bio());
        user.setUpdatedAt(Instant.now());
        userRepository.save(user);

        return new AuthResponse.UserInfo(
            user.getId(), user.getEmail(), user.getDisplayName(),
            user.getAvatarUrl(), user.getBio()
        );
    }

    // ─── Update avatar URL (called by Media_Service flow) ────────────────────

    public void updateAvatarUrl(String userId, String avatarUrl) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));
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
}
