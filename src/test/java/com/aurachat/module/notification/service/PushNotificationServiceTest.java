package com.aurachat.module.notification.service;

import com.aurachat.module.message.dto.MessageResponse;
import com.aurachat.module.presence.service.PresenceService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PushNotificationServiceTest {

    @Mock
    private PresenceService presenceService;

    @Mock
    private com.aurachat.module.auth.repository.UserRepository userRepository;

    @Mock
    private com.aurachat.module.auth.service.FcmTokenService fcmTokenService;

    @InjectMocks
    private PushNotificationService pushNotificationService;

    @Test
    void notifyNewMessage_skipsWhenRecipientOnline() {
        MessageResponse message = new MessageResponse(
            "msg-1",
            "conv-1",
            "sender-1",
            "TEXT",
            "Hello",
            null,
            null,
            null,
            List.of(),
            false,
            Instant.now()
        );

        when(presenceService.isOnline("recipient-1")).thenReturn(true);

        pushNotificationService.notifyNewMessage("recipient-1", message);

        verify(userRepository, never()).findById("recipient-1");
    }

    @Test
    void notifyNewMessage_skipsSender() {
        MessageResponse message = new MessageResponse(
            "msg-1",
            "conv-1",
            "user-1",
            "TEXT",
            "Hello",
            null,
            null,
            null,
            List.of(),
            false,
            Instant.now()
        );

        pushNotificationService.notifyNewMessage("user-1", message);

        verify(presenceService, never()).isOnline("user-1");
    }
}
