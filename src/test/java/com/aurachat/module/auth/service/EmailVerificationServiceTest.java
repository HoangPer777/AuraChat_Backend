package com.aurachat.module.auth.service;

import com.aurachat.common.exception.AuthenticationException;
import com.aurachat.module.auth.entity.User;
import com.aurachat.module.auth.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmailVerificationServiceTest {

    private static final String EMAIL = "user@example.com";

    @Mock
    private UserRepository userRepository;
    @Mock
    private StringRedisTemplate redisTemplate;
    @Mock
    private ValueOperations<String, String> valueOperations;
    @Mock
    private EmailService emailService;

    @InjectMocks
    private EmailVerificationService emailVerificationService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(emailVerificationService, "frontendUrl", "http://localhost:5173");
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    void verifyRegistrationEmail_marksUserVerified() {
        User user = User.builder()
            .email(EMAIL)
            .emailVerified(false)
            .build();
        when(valueOperations.get("verify:token:abc")).thenReturn(EMAIL);
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));

        emailVerificationService.verifyRegistrationEmail("abc");

        assertThat(user.getEmailVerified()).isTrue();
        verify(userRepository).save(user);
        verify(redisTemplate).delete("verify:token:abc");
    }

    @Test
    void verifyRegistrationEmail_rejectsInvalidToken() {
        when(valueOperations.get("verify:token:bad")).thenReturn(null);

        assertThatThrownBy(() -> emailVerificationService.verifyRegistrationEmail("bad"))
            .isInstanceOf(AuthenticationException.class);
    }

    @Test
    void sendRegistrationVerificationEmail_storesTokenAndSendsMail() {
        User user = User.builder()
            .email(EMAIL)
            .displayName("Test User")
            .build();

        emailVerificationService.sendRegistrationVerificationEmail(user);

        verify(valueOperations).set(startsWith("verify:token:"), eq(EMAIL), any(Duration.class));
        verify(emailService).send(eq(EMAIL), anyString(), anyString());
    }

    private static String startsWith(String prefix) {
        return org.mockito.ArgumentMatchers.argThat(value -> value != null && value.startsWith(prefix));
    }
}
