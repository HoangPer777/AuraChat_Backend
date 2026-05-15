package com.aurachat.module.media.integration;

import com.aurachat.common.response.DataResponse;
import com.aurachat.module.media.dto.MediaResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.imagekit.client.ImageKitClient;
import io.imagekit.models.files.FileUploadResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
    "imagekit.url-endpoint=https://ik.imagekit.io/test",
    "imagekit.public-key=test-public-key",
    "imagekit.private-key=test-private-key",
    "jwt.secret=test-jwt-secret-32-bytes-minimum-1234567890"
})
class MediaControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean(answer = org.mockito.Answers.RETURNS_DEEP_STUBS)
    private ImageKitClient imageKitClient;

    @MockBean
    private StringRedisTemplate redisTemplate;

    @MockBean
    private ValueOperations<String, String> valueOperations;

    @BeforeEach
    void setupMocks() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment(anyString())).thenReturn(1L);

        FileUploadResponse uploadResponse = mock(FileUploadResponse.class);
        when(uploadResponse.url()).thenReturn(Optional.of("https://ik.imagekit.io/test/upload.png"));
        when(imageKitClient.files().upload(any())).thenReturn(uploadResponse);
    }

    @Test
    void uploadImage_returnsMediaResponse() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "photo.png",
            MediaType.IMAGE_PNG_VALUE,
            "image-bytes".getBytes()
        );

        String responseBody = mockMvc.perform(
                multipart("/api/media/upload/image")
                    .file(file)
                    .with(SecurityMockMvcRequestPostProcessors.authentication(
                        new UsernamePasswordAuthenticationToken("user1", null, List.of())
                    ))
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andReturn()
            .getResponse()
            .getContentAsString();

        DataResponse<MediaResponse> response = objectMapper.readValue(
            responseBody,
            objectMapper.getTypeFactory().constructParametricType(DataResponse.class, MediaResponse.class)
        );

        assertThat(response.getData().url()).isEqualTo("https://ik.imagekit.io/test/upload.png");
    }

    @Test
    void uploadFile_returnsMediaResponse() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "doc.pdf",
            "application/pdf",
            "pdf".getBytes()
        );

        mockMvc.perform(
                multipart("/api/media/upload/file")
                    .file(file)
                    .with(SecurityMockMvcRequestPostProcessors.authentication(
                        new UsernamePasswordAuthenticationToken("user1", null, List.of())
                    ))
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.url").value("https://ik.imagekit.io/test/upload.png"));
    }
}
