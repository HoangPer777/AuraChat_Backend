package com.aurachat.module.message.pubsub;

import com.aurachat.module.message.dto.MessageResponse;
import com.aurachat.module.message.entity.Conversation;
import com.aurachat.module.message.repository.ConversationRepository;
import com.aurachat.module.notification.service.PushNotificationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class MessageSubscriber implements MessageListener {

    private final SimpMessagingTemplate messagingTemplate;
    private final ConversationRepository conversationRepository;
    private final ObjectMapper redisObjectMapper;
    private final RedisMessageListenerContainer listenerContainer;
    private final PushNotificationService pushNotificationService;

    @PostConstruct
    public void registerListener() {
        listenerContainer.addMessageListener(this, new PatternTopic("chat:*"));
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            String payload = new String(message.getBody());
            MessageResponse msg = redisObjectMapper.readValue(payload, MessageResponse.class);

            // Lấy danh sách thành viên và gửi đến từng người
            conversationRepository.findById(msg.conversationId()).ifPresent(conv -> {
                for (Conversation.Member member : conv.getMembers()) {
                    messagingTemplate.convertAndSendToUser(
                        member.getUserId(),
                        "/queue/messages",
                        msg
                    );

                    if (!member.getUserId().equals(msg.senderId())) {
                        pushNotificationService.notifyNewMessage(member.getUserId(), msg);
                    }
                }
            });
        } catch (Exception e) {
            log.error("Failed to process Redis message event: {}", e.getMessage());
        }
    }
}
