package com.aurachat.module.auth.service;

import com.aurachat.common.exception.AuthenticationException;
import com.aurachat.common.exception.BusinessLogicException;
import com.aurachat.common.exception.ErrorCode;
import com.aurachat.module.auth.entity.User;
import com.aurachat.module.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EmailVerificationService {

    private static final String VERIFY_TOKEN_PREFIX = "verify:token:";
    private static final Duration VERIFY_TOKEN_TTL = Duration.ofHours(24);

    private final UserRepository userRepository;
    private final StringRedisTemplate redisTemplate;
    private final EmailService emailService;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    /**
     * Gửi email xác thực cho tài khoản LOCAL mới đăng ký.
     */
    public void sendRegistrationVerificationEmail(User user) {
        String token = UUID.randomUUID().toString();
        redisTemplate.opsForValue().set(VERIFY_TOKEN_PREFIX + token, user.getEmail(), VERIFY_TOKEN_TTL);

        String link = frontendUrl + "/verify-email?token=" + token;
        emailService.send(
            user.getEmail(),
            "Aura Chat - Xác nhận email đăng ký",
            "Xin chào " + user.getDisplayName() + ",\n\n" +
            "Cảm ơn bạn đã đăng ký Aura Chat. Vui lòng nhấn vào liên kết sau để xác nhận email:\n\n" +
            link + "\n\n" +
            "Liên kết có hiệu lực trong 24 giờ.\n" +
            "Nếu bạn không đăng ký, hãy bỏ qua email này."
        );
    }

    /**
     * Xác thực email đăng ký qua token trong link.
     */
    public void verifyRegistrationEmail(String token) {
        String key = VERIFY_TOKEN_PREFIX + token;
        String email = redisTemplate.opsForValue().get(key);

        if (email == null) {
            throw new AuthenticationException(
                ErrorCode.AUTH_TOKEN_EXPIRED,
                "Verification link expired or invalid",
                "verify email"
            );
        }

        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new BusinessLogicException(
                ErrorCode.USER_NOT_FOUND,
                "User not found"
            ));

        user.setEmailVerified(true);
        user.setUpdatedAt(java.time.Instant.now());
        userRepository.save(user);
        redisTemplate.delete(key);
    }

    /**
     * Gửi lại email xác thực đăng ký.
     */
    public void resendVerificationEmail(String email) {
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new BusinessLogicException(
                ErrorCode.USER_NOT_FOUND,
                "User not found"
            ));

        if (!isLocalAccount(user)) {
            throw new BusinessLogicException(
                ErrorCode.AUTH_OAUTH_ACCOUNT,
                "Social login accounts do not require email verification"
            );
        }

        if (Boolean.TRUE.equals(user.getEmailVerified())) {
            throw new BusinessLogicException(
                ErrorCode.VALIDATION_FAILED,
                "Email is already verified"
            );
        }

        sendRegistrationVerificationEmail(user);
    }

    static boolean isLocalAccount(User user) {
        return "LOCAL".equalsIgnoreCase(user.getProvider()) && user.getPasswordHash() != null;
    }
}
