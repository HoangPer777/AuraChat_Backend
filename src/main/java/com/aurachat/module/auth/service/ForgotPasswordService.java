package com.aurachat.module.auth.service;

import com.aurachat.common.exception.AuthenticationException;
import com.aurachat.common.exception.BusinessLogicException;
import com.aurachat.common.exception.ErrorCode;
import com.aurachat.module.auth.dto.ResetPasswordRequest;
import com.aurachat.module.auth.entity.User;
import com.aurachat.module.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;

@Service
@RequiredArgsConstructor
public class ForgotPasswordService {

    private static final String OTP_PREFIX = "otp:forgot:";
    private static final Duration OTP_TTL = Duration.ofMinutes(10);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final StringRedisTemplate redisTemplate;
    private final JavaMailSender mailSender;

    /**
     * Tạo OTP 6 số, lưu Redis với TTL 10 phút, gửi email.
     * Luôn trả về thành công để tránh email enumeration.
     */
    public void sendOtp(String email) {
        userRepository.findByEmail(email).ifPresent(user -> {
            String otp = generateOtp();
            redisTemplate.opsForValue().set(OTP_PREFIX + email, otp, OTP_TTL);
            sendEmail(email, otp);
        });
    }

    /**
     * Xác minh OTP và đặt lại mật khẩu.
     */
    public void resetPassword(ResetPasswordRequest req) {
        String key = OTP_PREFIX + req.email();
        String storedOtp = redisTemplate.opsForValue().get(key);

        if (storedOtp == null) {
            throw new AuthenticationException(
                ErrorCode.AUTH_TOKEN_EXPIRED,
                "OTP expired or not found",
                "reset password"
            );
        }
        if (!storedOtp.equals(req.otp())) {
            throw new AuthenticationException(
                ErrorCode.AUTH_INVALID_CREDENTIALS,
                "Invalid OTP",
                "reset password"
            );
        }

        User user = userRepository.findByEmail(req.email())
            .orElseThrow(() -> new BusinessLogicException(
                ErrorCode.USER_NOT_FOUND,
                "User not found"
            ));

        user.setPasswordHash(passwordEncoder.encode(req.newPassword()));
        userRepository.save(user);

        // Xóa OTP sau khi dùng
        redisTemplate.delete(key);
    }

    private String generateOtp() {
        return String.format("%06d", new SecureRandom().nextInt(1_000_000));
    }

    private void sendEmail(String to, String otp) {
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setTo(to);
        msg.setSubject("Aura Chat - Mã xác nhận đặt lại mật khẩu");
        msg.setText(
            "Mã OTP của bạn là: " + otp + "\n\n" +
            "Mã có hiệu lực trong 10 phút.\n" +
            "Nếu bạn không yêu cầu, hãy bỏ qua email này."
        );
        mailSender.send(msg);
    }
}
