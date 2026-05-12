package com.aurachat.module.presence.listener;

import com.aurachat.module.presence.dto.PresenceStatusDto;
import com.aurachat.module.presence.pubsub.PresencePublisher;
import com.aurachat.module.presence.service.PresenceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.security.Principal;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketEventListener {

    private final PresenceService presenceService;
    private final PresencePublisher presencePublisher;

    @EventListener
    public void handleSessionConnected(SessionConnectedEvent event) {
        Principal principal = event.getUser();
        if (principal == null) return;

        String userId = principal.getName();
        presenceService.updatePresence(userId, "online");

        presencePublisher.publish(new PresenceStatusDto(userId, "online", null));
        log.info("WebSocket connected: userId={}", userId);
    }

    @EventListener
    public void handleSessionDisconnect(SessionDisconnectEvent event) {
        Principal principal = event.getUser();
        if (principal == null) return;

        String userId = principal.getName();
        presenceService.updatePresence(userId, "offline");
        presenceService.updateLastSeen(userId); // async

        presencePublisher.publish(new PresenceStatusDto(userId, "offline", null));
        log.info("WebSocket disconnected: userId={}", userId);
    }
}
