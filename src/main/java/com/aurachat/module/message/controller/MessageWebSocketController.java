package com.aurachat.module.message.controller;

import com.aurachat.module.message.dto.MessageResponse;
import com.aurachat.module.message.dto.SendMessageRequest;
import com.aurachat.module.message.service.MessageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

import java.security.Principal;

/**
 * WebSocket STOMP controller cho tin nhắn.
 * Client gửi đến /app/chat hoặc /app/seen.
 * Principal được inject tự động từ JwtChannelInterceptor.
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class MessageWebSocketController {

    private final MessageService messageService;

    /**
     * Gửi tin nhắn qua WebSocket.
     * Client: stompClient.publish({ destination: '/app/chat', body: JSON.stringify({...}) })
     */
    @MessageMapping("/chat")
    public void sendMessage(@Payload @Valid SendMessageRequest request, Principal principal) {
        String senderId = principal.getName();
        log.debug("WS /app/chat from userId={}, conversationId={}", senderId, request.conversationId());
        messageService.sendMessage(senderId, request);
    }

    /**
     * Đánh dấu tin nhắn đã xem qua WebSocket.
     * Client: stompClient.publish({ destination: '/app/seen', body: JSON.stringify({messageId, conversationId}) })
     */
    @MessageMapping("/seen")
    public void markAsSeen(@Payload SeenRequest request, Principal principal) {
        String userId = principal.getName();
        log.debug("WS /app/seen from userId={}, messageId={}", userId, request.messageId());
        messageService.markAsSeen(request.messageId(), userId, request.conversationId());
    }

    public record SeenRequest(String messageId, String conversationId) {}
}
