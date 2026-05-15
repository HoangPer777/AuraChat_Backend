package com.aurachat.module.media.service;

import com.aurachat.common.exception.ValidationException;
import com.aurachat.module.media.dto.MediaResponse;
import io.imagekit.client.ImageKitClient;
import io.imagekit.models.files.FileUploadResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.mock.web.MockMultipartFile;

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

        MediaResponse response = mediaService.uploadImage(file, "user1");

        assertThat(response.url()).isEqualTo("https://ik.imagekit.io/test/photo.png");
        assertThat(response.contentType()).isEqualTo("image/png");
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
}
