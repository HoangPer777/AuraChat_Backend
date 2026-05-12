package com.aurachat.module.presence.service;

import com.aurachat.module.auth.repository.UserRepository;
import com.aurachat.module.presence.dto.PresenceStatusDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PresenceService {

    private static final String PRESENCE_KEY_PREFIX = "presence:";
    private static final Duration PRESENCE_TTL = Duration.ofSeconds(30);

    private final RedisTemplate<String, String> redisTemplate;
    private final UserRepository userRepository;

    // ─── Update ───────────────────────────────────────────────────────────────

    /**
     * Cập nhật trạng thái presence.
     * status="online" → SET key với TTL 30s
     * status="offline" → DEL key
     */
    public void updatePresence(String userId, String status) {
        String key = PRESENCE_KEY_PREFIX + userId;
        if ("online".equals(status)) {
            redisTemplate.opsForValue().set(key, "online", PRESENCE_TTL);
            log.debug("Presence set online: userId={}", userId);
        } else {
            redisTemplate.delete(key);
            log.debug("Presence set offline: userId={}", userId);
        }
    }

    /**
     * Gia hạn TTL của presence key thêm 30 giây (heartbeat).
     */
    public void refreshTTL(String userId) {
        String key = PRESENCE_KEY_PREFIX + userId;
        Boolean exists = redisTemplate.hasKey(key);
        if (Boolean.TRUE.equals(exists)) {
            redisTemplate.expire(key, PRESENCE_TTL);
            log.debug("Presence TTL refreshed: userId={}", userId);
        } else {
            // Key đã hết hạn — tạo lại
            redisTemplate.opsForValue().set(key, "online", PRESENCE_TTL);
            log.debug("Presence key recreated on heartbeat: userId={}", userId);
        }
    }

    // ─── Query ────────────────────────────────────────────────────────────────

    public boolean isOnline(String userId) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(PRESENCE_KEY_PREFIX + userId));
    }

    public PresenceStatusDto getPresenceStatus(String userId) {
        boolean online = isOnline(userId);
        Instant lastSeen = null;
        if (!online) {
            lastSeen = userRepository.findById(userId)
                .map(u -> u.getLastSeen())
                .orElse(null);
        }
        return new PresenceStatusDto(userId, online ? "online" : "offline", lastSeen);
    }

    public List<String> getOnlineFriendIds(List<String> friendIds) {
        return friendIds.stream()
            .filter(this::isOnline)
            .toList();
    }

    // ─── Async ────────────────────────────────────────────────────────────────

    @Async
    public void updateLastSeen(String userId) {
        userRepository.findById(userId).ifPresent(user -> {
            user.setLastSeen(Instant.now());
            userRepository.save(user);
            log.debug("lastSeen updated: userId={}", userId);
        });
    }
}
