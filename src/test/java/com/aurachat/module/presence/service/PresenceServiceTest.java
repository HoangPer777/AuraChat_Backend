package com.aurachat.module.presence.service;

import com.aurachat.module.auth.entity.User;
import com.aurachat.module.auth.repository.UserRepository;
import com.aurachat.module.presence.dto.PresenceStatusDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PresenceServiceTest {

    @Mock
    private RedisTemplate<String, String> redisTemplate;
    @Mock
    private ValueOperations<String, String> valueOps;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private PresenceService presenceService;

    private static final String USER_ID = "user1";
    private static final String PRESENCE_KEY = "presence:" + USER_ID;

    // ─── updatePresence ───────────────────────────────────────────────────────

    @Test
    void updatePresence_online_setsRedisKeyWithTTL() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);

        presenceService.updatePresence(USER_ID, "online");

        verify(valueOps).set(eq(PRESENCE_KEY), eq("online"), eq(Duration.ofSeconds(30)));
    }

    @Test
    void updatePresence_offline_deletesRedisKey() {
        presenceService.updatePresence(USER_ID, "offline");

        verify(redisTemplate).delete(PRESENCE_KEY);
    }

    // ─── isOnline ─────────────────────────────────────────────────────────────

    @Test
    void isOnline_returnsTrueWhenKeyExists() {
        when(redisTemplate.hasKey(PRESENCE_KEY)).thenReturn(true);

        assertThat(presenceService.isOnline(USER_ID)).isTrue();
    }

    @Test
    void isOnline_returnsFalseWhenKeyNotExists() {
        when(redisTemplate.hasKey(PRESENCE_KEY)).thenReturn(false);

        assertThat(presenceService.isOnline(USER_ID)).isFalse();
    }

    // ─── refreshTTL ───────────────────────────────────────────────────────────

    @Test
    void refreshTTL_extendsExistingKey() {
        when(redisTemplate.hasKey(PRESENCE_KEY)).thenReturn(true);

        presenceService.refreshTTL(USER_ID);

        verify(redisTemplate).expire(eq(PRESENCE_KEY), eq(Duration.ofSeconds(30)));
    }

    @Test
    void refreshTTL_recreatesKeyWhenExpired() {
        when(redisTemplate.hasKey(PRESENCE_KEY)).thenReturn(false);
        when(redisTemplate.opsForValue()).thenReturn(valueOps);

        presenceService.refreshTTL(USER_ID);

        verify(valueOps).set(eq(PRESENCE_KEY), eq("online"), eq(Duration.ofSeconds(30)));
    }

    // ─── getPresenceStatus ────────────────────────────────────────────────────

    @Test
    void getPresenceStatus_returnsOnlineWhenKeyExists() {
        when(redisTemplate.hasKey(PRESENCE_KEY)).thenReturn(true);

        PresenceStatusDto result = presenceService.getPresenceStatus(USER_ID);

        assertThat(result.status()).isEqualTo("online");
        assertThat(result.lastSeen()).isNull();
    }

    @Test
    void getPresenceStatus_returnsOfflineWithLastSeenFromMongoDB() {
        when(redisTemplate.hasKey(PRESENCE_KEY)).thenReturn(false);
        Instant lastSeen = Instant.now().minusSeconds(60);
        User user = new User();
        user.setLastSeen(lastSeen);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));

        PresenceStatusDto result = presenceService.getPresenceStatus(USER_ID);

        assertThat(result.status()).isEqualTo("offline");
        assertThat(result.lastSeen()).isEqualTo(lastSeen);
    }
}
