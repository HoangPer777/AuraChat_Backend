package com.aurachat.module.auth.service;

import com.aurachat.common.exception.AuthenticationException;
import com.aurachat.module.auth.dto.ForgotPasswordResponse;
import com.aurachat.module.auth.dto.ResetPasswordRequest;
import com.aurachat.module.auth.entity.User;
import com.aurachat.module.auth.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ForgotPasswordServiceTest {

    private static final String EMAIL = "user@example.com";

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private StringRedisTemplate redisTemplate;
    @Mock
    private ValueOperations<String, String> valueOperations;
    @Mock
    private EmailService emailService;

    @InjectMocks
    private ForgotPasswordService forgotPasswordService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(forgotPasswordService, "frontendUrl", "http://localhost:5173");
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    void requestResetLink_returnsGenericSuccessWhenUserNotFound() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

        ForgotPasswordResponse response = forgotPasswordService.requestResetLink(EMAIL);

        assertThat(response.status()).isEqualTo("SENT");
        verify(emailService, never()).send(anyString(), anyString(), anyString());
    }

    @Test
    void requestResetLink_returnsOAuthMessageForSocialAccount() {
        User user = User.builder()
            .email(EMAIL)
            .provider("GOOGLE")
            .passwordHash(null)
            .build();
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));

        ForgotPasswordResponse response = forgotPasswordService.requestResetLink(EMAIL);

        assertThat(response.status()).isEqualTo("OAUTH");
        assertThat(response.provider()).isEqualTo("GOOGLE");
        verify(emailService, never()).send(anyString(), anyString(), anyString());
    }

    @Test
    void requestResetLink_sendsEmailForLocalAccount() {
        User user = User.builder()
            .email(EMAIL)
            .provider("LOCAL")
            .passwordHash("hash")
            .build();
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));

        ForgotPasswordResponse response = forgotPasswordService.requestResetLink(EMAIL);

        assertThat(response.status()).isEqualTo("SENT");
        assertThat(response.provider()).isEqualTo("LOCAL");
        verify(valueOperations).set(startsWith("forgot:token:"), eq(EMAIL), any(Duration.class));
        verify(emailService).send(eq(EMAIL), anyString(), anyString());
    }

    @Test
    void verifyResetEmail_marksEmailAsVerified() {
        when(valueOperations.get("forgot:token:abc")).thenReturn(EMAIL);

        String email = forgotPasswordService.verifyResetEmail("abc");

        assertThat(email).isEqualTo(EMAIL);
        verify(valueOperations).set(eq("forgot:verified:" + EMAIL), eq("1"), any(Duration.class));
        verify(redisTemplate).delete("forgot:token:abc");
    }

    @Test
    void resetPassword_requiresVerifiedEmail() {
        when(redisTemplate.hasKey("forgot:verified:" + EMAIL)).thenReturn(false);

        assertThatThrownBy(() -> forgotPasswordService.resetPassword(
            new ResetPasswordRequest(EMAIL, "newpassword123")
        )).isInstanceOf(AuthenticationException.class);
    }

    @Test
    void resetPassword_updatesPasswordWhenVerified() {
        User user = User.builder()
            .email(EMAIL)
            .provider("LOCAL")
            .passwordHash("old")
            .build();
        when(redisTemplate.hasKey("forgot:verified:" + EMAIL)).thenReturn(true);
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(passwordEncoder.encode("newpassword123")).thenReturn("encoded");

        forgotPasswordService.resetPassword(new ResetPasswordRequest(EMAIL, "newpassword123"));

        assertThat(user.getPasswordHash()).isEqualTo("encoded");
        verify(userRepository).save(user);
        verify(redisTemplate).delete("forgot:verified:" + EMAIL);
    }

    private static String startsWith(String prefix) {
        return org.mockito.ArgumentMatchers.argThat(value -> value != null && value.startsWith(prefix));
    }
}
