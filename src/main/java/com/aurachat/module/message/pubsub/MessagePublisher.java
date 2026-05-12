package com.aurachat.module.message.pubsub;

import com.aurachat.module.message.dto.MessageResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class MessagePublisher {

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper redisObjectMapper;

    public void publish(String conversationId, MessageResponse msg) {
        try {
            String payload = redisObjectMapper.writeValueAsString(msg);
            redisTemplate.convertAndSend("chat:" + conversationId, payload);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize message for Redis pub/sub: {}", e.getMessage());
        }
    }
}
