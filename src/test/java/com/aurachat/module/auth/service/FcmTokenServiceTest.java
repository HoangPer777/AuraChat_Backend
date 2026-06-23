package com.aurachat.module.auth.service;

import com.aurachat.common.exception.BusinessLogicException;
import com.aurachat.module.auth.entity.User;
import com.aurachat.module.auth.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FcmTokenServiceTest {

    private static final String USER_ID = "user-1";

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private FcmTokenService fcmTokenService;

    @Test
    void registerToken_addsTokenToUser() {
        User user = User.builder()
            .id(USER_ID)
            .fcmTokens(new ArrayList<>())
            .build();
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        fcmTokenService.registerToken(USER_ID, "token-a");

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getFcmTokens()).containsExactly("token-a");
    }

    @Test
    void registerToken_movesExistingTokenToEnd() {
        User user = User.builder()
            .id(USER_ID)
            .fcmTokens(new ArrayList<>(List.of("token-a", "token-b")))
            .build();
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        fcmTokenService.registerToken(USER_ID, "token-a");

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getFcmTokens()).containsExactly("token-b", "token-a");
    }

    @Test
    void registerToken_throwsWhenUserMissing() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> fcmTokenService.registerToken(USER_ID, "token-a"))
            .isInstanceOf(BusinessLogicException.class);
    }

    @Test
    void removeToken_removesMatchingToken() {
        User user = User.builder()
            .id(USER_ID)
            .fcmTokens(new ArrayList<>(List.of("token-a", "token-b")))
            .build();
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        fcmTokenService.removeToken(USER_ID, "token-a");

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getFcmTokens()).containsExactly("token-b");
    }
}
