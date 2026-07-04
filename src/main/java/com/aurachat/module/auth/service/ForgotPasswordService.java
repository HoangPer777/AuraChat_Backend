package com.aurachat.module.auth.service;

import com.aurachat.common.exception.AuthenticationException;
import com.aurachat.common.exception.BusinessLogicException;
import com.aurachat.common.exception.ErrorCode;
import com.aurachat.module.auth.dto.ForgotPasswordResponse;
import com.aurachat.module.auth.dto.ResetPasswordRequest;
import com.aurachat.module.auth.entity.User;
import com.aurachat.module.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ForgotPasswordService {

    private static final String FORGOT_TOKEN_PREFIX = "forgot:token:";
    private static final String FORGOT_VERIFIED_PREFIX = "forgot:verified:";
    private static final Duration FORGOT_TTL = Duration.ofHours(1);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final StringRedisTemplate redisTemplate;
    private final EmailService emailService;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    /**
     * Kiểm tra phương thức đăng nhập và gửi link xác thực email nếu là tài khoản LOCAL.
     */
    public ForgotPasswordResponse requestResetLink(String email) {
        Optional<User> userOpt = userRepository.findByEmail(email);

        if (userOpt.isEmpty()) {
            return ForgotPasswordResponse.genericSuccess();
        }

        User user = userOpt.get();
        if (!EmailVerificationService.isLocalAccount(user)) {
            return ForgotPasswordResponse.oauthAccount(user.getProvider());
        }

        sendResetLinkEmail(email);
        return ForgotPasswordResponse.linkSent();
    }

    /**
     * Xác thực email qua link trong email quên mật khẩu.
     */
    public String verifyResetEmail(String token) {
        String key = FORGOT_TOKEN_PREFIX + token;
        String email = redisTemplate.opsForValue().get(key);

        if (email == null) {
            throw new AuthenticationException(
                ErrorCode.AUTH_TOKEN_EXPIRED,
                "Reset link expired or invalid",
                "verify forgot password"
            );
        }

        redisTemplate.opsForValue().set(FORGOT_VERIFIED_PREFIX + email, "1", FORGOT_TTL);
        redisTemplate.delete(key);
        return email;
    }

    /**
     * Kiểm tra email đã được xác thực cho phiên đặt lại mật khẩu chưa.
     */
    public boolean isResetEmailVerified(String email) {
        return Boolean.TRUE.equals(
            redisTemplate.hasKey(FORGOT_VERIFIED_PREFIX + email)
        );
    }

    /**
     * Đặt lại mật khẩu sau khi email đã được xác thực.
     */
    public void resetPassword(ResetPasswordRequest req) {
        if (!isResetEmailVerified(req.email())) {
            throw new AuthenticationException(
                ErrorCode.AUTH_TOKEN_INVALID,
                "Email not verified for password reset",
                "reset password"
            );
        }

        User user = userRepository.findByEmail(req.email())
            .orElseThrow(() -> new BusinessLogicException(
                ErrorCode.USER_NOT_FOUND,
                "User not found"
            ));

        if (!EmailVerificationService.isLocalAccount(user)) {
            throw new BusinessLogicException(
                ErrorCode.AUTH_OAUTH_ACCOUNT,
                "Password reset is not available for this account"
            );
        }

        user.setPasswordHash(passwordEncoder.encode(req.newPassword()));
        user.setUpdatedAt(java.time.Instant.now());
        userRepository.save(user);

        redisTemplate.delete(FORGOT_VERIFIED_PREFIX + req.email());
    }

    private void sendResetLinkEmail(String email) {
        String token = UUID.randomUUID().toString();
        redisTemplate.delete(FORGOT_VERIFIED_PREFIX + email);
        redisTemplate.opsForValue().set(FORGOT_TOKEN_PREFIX + token, email, FORGOT_TTL);

        String link = frontendUrl + "/verify-forgot-password?token=" + token;
        emailService.send(
            email,
            "Aura Chat - Xác nhận email đặt lại mật khẩu",
            "Bạn đã yêu cầu đặt lại mật khẩu Aura Chat.\n\n" +
            "Vui lòng nhấn vào liên kết sau để xác nhận email:\n\n" +
            link + "\n\n" +
            "Liên kết có hiệu lực trong 1 giờ.\n" +
            "Nếu bạn không yêu cầu, hãy bỏ qua email này."
        );
    }
}
