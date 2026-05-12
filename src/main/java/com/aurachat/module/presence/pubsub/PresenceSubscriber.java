package com.aurachat.module.presence.pubsub;

import com.aurachat.module.friend.repository.FriendshipRepository;
import com.aurachat.module.presence.dto.PresenceStatusDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PresenceSubscriber implements MessageListener {

    private final SimpMessagingTemplate messagingTemplate;
    private final FriendshipRepository friendshipRepository;
    private final ObjectMapper redisObjectMapper;
    private final RedisMessageListenerContainer listenerContainer;

    @PostConstruct
    public void registerListener() {
        listenerContainer.addMessageListener(this, new ChannelTopic("presence"));
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            String payload = new String(message.getBody());
            PresenceStatusDto dto = redisObjectMapper.readValue(payload, PresenceStatusDto.class);

            // Gửi cập nhật đến tất cả bạn bè của người dùng
            friendshipRepository.findByUserId(dto.userId()).forEach(friendship -> {
                messagingTemplate.convertAndSendToUser(
                    friendship.getFriendId(),
                    "/queue/presence",
                    dto
                );
            });
        } catch (Exception e) {
            log.error("Failed to process Redis presence event: {}", e.getMessage());
        }
    }
}
