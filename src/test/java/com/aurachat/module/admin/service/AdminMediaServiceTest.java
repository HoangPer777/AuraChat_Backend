package com.aurachat.module.admin.service;

import com.aurachat.common.exception.BusinessLogicException;
import com.aurachat.module.auth.entity.User;
import com.aurachat.module.auth.repository.UserRepository;
import com.aurachat.module.media.entity.Media;
import com.aurachat.module.media.service.MediaService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminMediaServiceTest {

    @Mock MongoTemplate mongoTemplate;
    @Mock UserRepository userRepository;
    @Mock MediaService mediaService;
    @InjectMocks AdminMediaService adminMediaService;

    @Test
    void getMediaById_returnsOwnerInfo() {
        Media media = media("media-1", "user-1");
        User owner = user("user-1", "Alice", "alice@test.com");
        when(mongoTemplate.findById("media-1", Media.class)).thenReturn(media);
        when(userRepository.findById("user-1")).thenReturn(Optional.of(owner));

        var result = adminMediaService.getMediaById("media-1");

        assertThat(result.id()).isEqualTo("media-1");
        assertThat(result.ownerDisplayName()).isEqualTo("Alice");
        assertThat(result.ownerEmail()).isEqualTo("alice@test.com");
    }

    @Test
    void getMediaById_throwsWhenMissing() {
        when(mongoTemplate.findById("missing", Media.class)).thenReturn(null);

        assertThatThrownBy(() -> adminMediaService.getMediaById("missing"))
            .isInstanceOf(BusinessLogicException.class);
    }

    @Test
    void deleteMedia_delegatesToMediaService() {
        adminMediaService.deleteMedia("media-1", "admin-1");
        verify(mediaService).adminDeleteMedia("media-1", "admin-1");
    }

    @Test
    void getStats_aggregatesCounts() {
        when(mongoTemplate.count(any(), eq(Media.class))).thenReturn(10L, 8L);
        when(mongoTemplate.aggregate(any(), eq(Media.class), eq(AdminMediaService.SizeSumResult.class)))
            .thenReturn(new org.springframework.data.mongodb.core.aggregation.AggregationResults<>(
                List.of(new AdminMediaService.SizeSumResult(2048L)), Media.class));

        var stats = adminMediaService.getStats();

        assertThat(stats.totalCount()).isEqualTo(10);
        assertThat(stats.activeCount()).isEqualTo(8);
        assertThat(stats.totalBytes()).isEqualTo(2048L);
    }

    private Media media(String id, String ownerId) {
        return Media.builder()
            .id(id)
            .ownerId(ownerId)
            .url("https://ik.imagekit.io/test.png")
            .fileName("test.png")
            .originalFileName("photo.png")
            .contentType("image/png")
            .size(1024)
            .mediaType("IMAGE")
            .provider("ImageKit")
            .createdAt(Instant.now())
            .build();
    }

    private User user(String id, String name, String email) {
        User user = new User();
        user.setId(id);
        user.setDisplayName(name);
        user.setEmail(email);
        return user;
    }
}
