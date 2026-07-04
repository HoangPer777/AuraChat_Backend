package com.aurachat.module.auth.service;

import com.aurachat.common.exception.AuthenticationException;
import com.aurachat.common.exception.BusinessLogicException;
import com.aurachat.common.exception.ErrorCode;
import com.aurachat.config.JwtUtil;
import com.aurachat.module.auth.dto.AuthResponse;
import com.aurachat.module.auth.dto.FirebaseLoginRequest;
import com.aurachat.module.auth.entity.RefreshToken;
import com.aurachat.module.auth.entity.User;
import com.aurachat.module.auth.repository.RefreshTokenRepository;
import com.aurachat.module.auth.repository.UserRepository;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

/**
 * Service for handling Firebase authentication.
 * Verifies Firebase ID tokens and manages user authentication.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FirebaseAuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtUtil jwtUtil;

    /**
     * Authenticates user with Firebase ID token.
     * Creates or updates user based on Firebase token information.
     *
     * @param request Firebase login request containing ID token
     * @return AuthResponse with JWT tokens and user info
     */
    public AuthResponse loginWithFirebase(FirebaseLoginRequest request) {
        try {
            // Verify Firebase ID token
            FirebaseToken decodedToken = FirebaseAuth.getInstance()
                .verifyIdToken(request.idToken());

            // Extract user information from Firebase token
            String firebaseUid = decodedToken.getUid();
            String email = decodedToken.getEmail();
            String name = decodedToken.getName();
            String picture = decodedToken.getPicture();
            String provider = determineProvider(decodedToken);

            log.info("Firebase authentication successful for user: {} ({})", email, provider);

            // Find or create user
            User user = findOrCreateUser(firebaseUid, email, name, picture, provider);

            if (!"ACTIVE".equalsIgnoreCase(user.getStatus())) {
                throw new AuthenticationException(
                    ErrorCode.AUTH_ACCOUNT_LOCKED,
                    "Account status is " + user.getStatus(),
                    "firebase authentication"
                );
            }

            // Generate JWT tokens
            String accessToken = jwtUtil.generateAccessToken(user.getId());
            String refreshToken = createRefreshToken(user.getId());

            return AuthResponse.of(accessToken, refreshToken, user);

        } catch (AuthenticationException e) {
            throw e;
        } catch (FirebaseAuthException e) {
            log.error("Firebase token verification failed: {}", e.getMessage());
            throw new AuthenticationException(
                ErrorCode.AUTH_TOKEN_INVALID,
                "Invalid Firebase ID token: " + e.getMessage(),
                "firebase authentication"
            );
        } catch (Exception e) {
            log.error("Firebase authentication failed: {}", e.getMessage(), e);
            throw new AuthenticationException(
                ErrorCode.AUTH_FAILED,
                "Firebase authentication failed",
                "firebase authentication"
            );
        }
    }

    /**
     * Determines the authentication provider from Firebase token.
     *
     * @param token Firebase token
     * @return Provider name (GOOGLE, FACEBOOK, etc.)
     */
    private String determineProvider(FirebaseToken token) {
        // Firebase token contains provider information in the firebase claim
        Object firebaseClaim = token.getClaims().get("firebase");
        if (firebaseClaim instanceof java.util.Map<?, ?>) {
            @SuppressWarnings("unchecked")
            var firebaseClaims = (java.util.Map<String, Object>) firebaseClaim;
            String provider = (String) firebaseClaims.get("sign_in_provider");
            if (provider != null) {
                return mapFirebaseProvider(provider);
            }
        }

        // Fallback: try to determine from other claims
        String authTime = token.getClaims().get("auth_time") != null ? "FIREBASE" : "UNKNOWN";
        return authTime;
    }

    /**
     * Maps Firebase provider names to our internal provider names.
     *
     * @param firebaseProvider Firebase provider name
     * @return Internal provider name
     */
    private String mapFirebaseProvider(String firebaseProvider) {
        return switch (firebaseProvider) {
            case "google.com" -> "GOOGLE";
            case "facebook.com" -> "FACEBOOK";
            case "password" -> "FIREBASE_EMAIL";
            default -> "FIREBASE";
        };
    }

    /**
     * Finds existing user or creates new user based on Firebase information.
     *
     * @param firebaseUid Firebase UID
     * @param email User email
     * @param name User display name
     * @param picture User profile picture URL
     * @param provider Authentication provider
     * @return User entity
     */
    private User findOrCreateUser(String firebaseUid, String email, String name, String picture, String provider) {
        // First, try to find user by Firebase UID
        User user = userRepository.findByProviderId(firebaseUid).orElse(null);

        if (user != null) {
            // Update existing user information
            updateUserInfo(user, email, name, picture, provider);
            return user;
        }

        // If not found by UID, try to find by email
        user = userRepository.findByEmail(email).orElse(null);

        if (user != null) {
            // Link existing email account with Firebase
            user.setProvider(provider);
            user.setProviderId(firebaseUid);
            updateUserInfo(user, email, name, picture, provider);
            return user;
        }

        // Create new user
        return createNewUser(firebaseUid, email, name, picture, provider);
    }

    /**
     * Updates existing user information.
     *
     * @param user Existing user
     * @param email User email
     * @param name User display name
     * @param picture User profile picture URL
     * @param provider Authentication provider
     */
    private void updateUserInfo(User user, String email, String name, String picture, String provider) {
        boolean updated = false;

        if (name != null && !name.equals(user.getDisplayName())) {
            user.setDisplayName(name);
            updated = true;
        }

        if (picture != null && !picture.equals(user.getAvatarUrl())) {
            user.setAvatarUrl(picture);
            updated = true;
        }

        if (updated) {
            user.setUpdatedAt(Instant.now());
            userRepository.save(user);
            log.info("Updated user information for: {}", email);
        }
    }

    /**
     * Creates a new user from Firebase information.
     *
     * @param firebaseUid Firebase UID
     * @param email User email
     * @param name User display name
     * @param picture User profile picture URL
     * @param provider Authentication provider
     * @return New user entity
     */
    private User createNewUser(String firebaseUid, String email, String name, String picture, String provider) {
        if (email == null) {
            throw new BusinessLogicException(
                ErrorCode.VALIDATION_REQUIRED_FIELD,
                "Email is required for user registration"
            );
        }

        User user = User.builder()
            .email(email)
            .displayName(name != null ? name : email.split("@")[0])
            .avatarUrl(picture)
            .provider(provider)
            .providerId(firebaseUid)
            .emailVerified(true)
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();

        userRepository.save(user);
        log.info("Created new user from Firebase: {} ({})", email, provider);

        return user;
    }

    /**
     * Creates a refresh token for the user.
     *
     * @param userId User ID
     * @return Refresh token string
     */
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
