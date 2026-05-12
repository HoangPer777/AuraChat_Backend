package com.aurachat.module.presence.property;

import com.aurachat.module.auth.repository.UserRepository;
import com.aurachat.module.presence.service.PresenceService;
import net.jqwik.api.*;
import net.jqwik.api.constraints.NotBlank;
import org.mockito.Mockito;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Property-based tests cho Presence module.
 */
class PresencePropertyTest {

    private final RedisTemplate<String, String> redisTemplate = Mockito.mock(RedisTemplate.class);
    private final ValueOperations<String, String> valueOps = Mockito.mock(ValueOperations.class);
    private final UserRepository userRepository = Mockito.mock(UserRepository.class);
    private final PresenceService presenceService = new PresenceService(redisTemplate, userRepository);

    /**
     * Property: Sau khi TTL hết hạn (key không tồn tại trong Redis),
     * isOnline phải trả về false.
     * Mô phỏng: key không tồn tại = TTL đã hết hạn.
     */
    @Property
    void presenceShouldBeOfflineWhenKeyExpired(@ForAll @NotBlank String userId) {
        // Simulate: Redis key không tồn tại (TTL đã hết hạn)
        when(redisTemplate.hasKey("presence:" + userId)).thenReturn(false);

        assertThat(presenceService.isOnline(userId)).isFalse();
    }

    /**
     * Property: Sau khi updatePresence("online"), isOnline phải trả về true.
     */
    @Property
    void presenceShouldBeOnlineAfterUpdate(@ForAll @NotBlank String userId) {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        // Sau khi set, key tồn tại
        when(redisTemplate.hasKey("presence:" + userId)).thenReturn(true);

        presenceService.updatePresence(userId, "online");
        assertThat(presenceService.isOnline(userId)).isTrue();
    }

    /**
     * Property: updatePresence("offline") luôn xóa key.
     * Với bất kỳ userId nào, sau khi set offline, key phải bị xóa.
     */
    @Property
    void updatePresenceOfflineAlwaysDeletesKey(@ForAll @NotBlank String userId) {
        presenceService.updatePresence(userId, "offline");

        verify(redisTemplate).delete("presence:" + userId);
    }

    /**
     * Property: refreshTTL luôn set TTL = 30 giây.
     */
    @Property
    void refreshTTLAlwaysSets30SecondsTTL(@ForAll @NotBlank String userId) {
        when(redisTemplate.hasKey("presence:" + userId)).thenReturn(true);

        presenceService.refreshTTL(userId);

        verify(redisTemplate).expire(eq("presence:" + userId), eq(Duration.ofSeconds(30)));
    }
}
