package com.aurachat.module.presence.controller;

import com.aurachat.module.presence.service.PresenceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;

import java.security.Principal;

/**
 * WebSocket STOMP controller cho heartbeat presence.
 * Client gửi đến /app/presence/heartbeat mỗi 20 giây.
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class PresenceWebSocketController {

    private final PresenceService presenceService;

    @MessageMapping("/presence/heartbeat")
    public void heartbeat(Principal principal) {
        if (principal == null) return;
        String userId = principal.getName();
        presenceService.refreshTTL(userId);
        log.debug("Heartbeat received: userId={}", userId);
    }
}
