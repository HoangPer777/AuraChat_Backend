package com.aurachat.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Xác thực JWT trong STOMP CONNECT frame.
 * Sau khi xác thực, set Principal vào session để các @MessageMapping handler
 * có thể inject Principal và lấy userId.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtChannelInterceptor implements ChannelInterceptor {

    private final JwtUtil jwtUtil;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            String authHeader = accessor.getFirstNativeHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                if (jwtUtil.isValid(token)) {
                    String userId = jwtUtil.extractUserId(token);
                    var auth = new UsernamePasswordAuthenticationToken(userId, null, List.of());
                    accessor.setUser(auth);
                    log.debug("WebSocket authenticated: userId={}", userId);
                } else {
                    log.warn("WebSocket CONNECT rejected: invalid JWT token");
                    // Trả về null để từ chối kết nối
                    return null;
                }
            } else {
                log.warn("WebSocket CONNECT rejected: missing Authorization header");
                return null;
            }
        }

        return message;
    }
}
