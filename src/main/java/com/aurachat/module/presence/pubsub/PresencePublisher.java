package com.aurachat.module.presence.pubsub;

import com.aurachat.module.presence.dto.PresenceStatusDto;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class PresencePublisher {

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper redisObjectMapper;

    public void publish(PresenceStatusDto dto) {
        try {
            String payload = redisObjectMapper.writeValueAsString(dto);
            redisTemplate.convertAndSend("presence", payload);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize presence event for Redis pub/sub: {}", e.getMessage());
        }
    }
}
