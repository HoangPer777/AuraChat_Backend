package com.aurachat.module.media.service;

import com.aurachat.common.exception.ValidationException;
import com.aurachat.module.media.dto.MediaPageResponse;
import com.aurachat.module.media.dto.MediaResponse;
import com.aurachat.module.media.entity.Media;
import com.aurachat.module.media.repository.MediaRepository;
import io.imagekit.client.ImageKitClient;
import io.imagekit.models.files.FileUploadResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.mock.web.MockMultipartFile;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MediaServiceTest {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private ImageKitClient imageKit;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private MediaRepository mediaRepository;

    @InjectMocks
    private MediaService mediaService;

    @BeforeEach
    void setupRedis() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment(anyString())).thenReturn(1L);
    }

    @Test
    void uploadImage_uploadsToImageKit() {
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "photo.png",
            "image/png",
            "image-bytes".getBytes()
        );

        FileUploadResponse uploadResponse = mock(FileUploadResponse.class);
        when(uploadResponse.url()).thenReturn(Optional.of("https://ik.imagekit.io/test/photo.png"));
        when(imageKit.files().upload(any())).thenReturn(uploadResponse);
        when(mediaRepository.save(any(Media.class))).thenAnswer(invocation -> {
            Media media = invocation.getArgument(0);
            media.setId("media-1");
            return media;
        });

        MediaResponse response = mediaService.uploadImage(file, "user1");

        assertThat(response.url()).isEqualTo("https://ik.imagekit.io/test/photo.png");
        assertThat(response.contentType()).isEqualTo("image/png");
        assertThat(response.id()).isEqualTo("media-1");
        verify(imageKit.files()).upload(any());
    }

    @Test
    void uploadFile_rejectsExecutableExtension() {
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "tool.exe",
            "application/octet-stream",
            "bin".getBytes()
        );

        assertThatThrownBy(() -> mediaService.uploadFile(file, "user1"))
            .isInstanceOf(ValidationException.class);
    }

    @Test
    void uploadImage_rejectsInvalidContentType() {
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "note.txt",
            "text/plain",
            "hello".getBytes()
        );

        assertThatThrownBy(() -> mediaService.uploadImage(file, "user1"))
            .isInstanceOf(ValidationException.class);
    }

    @Test
    void uploadFile_rateLimitExceeded() {
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "doc.pdf",
            "application/pdf",
            "pdf".getBytes()
        );
        when(valueOperations.increment(anyString())).thenReturn(11L);

        assertThatThrownBy(() -> mediaService.uploadFile(file, "user1"))
            .isInstanceOf(ValidationException.class);
    }

    @Test
    void getUserMedia_returnsPagedResponse() {
        Media media = Media.builder()
            .id("m1")
            .ownerId("user1")
            .url("https://ik.imagekit.io/test/photo.png")
            .fileName("media_file.png")
            .originalFileName("photo.png")
            .contentType("image/png")
            .size(123L)
            .provider("ImageKit")
            .mediaType("IMAGE")
            .createdAt(Instant.now())
            .build();
        when(mediaRepository.findByOwnerIdAndDeletedFalseOrderByCreatedAtDesc(eq("user1"), any()))
            .thenReturn(new PageImpl<>(List.of(media), PageRequest.of(0, 20), 1));

        MediaPageResponse response = mediaService.getUserMedia("user1", 0, 20);
        assertThat(response.totalElements()).isEqualTo(1);
        assertThat(response.content()).hasSize(1);
        assertThat(response.content().get(0).id()).isEqualTo("m1");
    }
}
