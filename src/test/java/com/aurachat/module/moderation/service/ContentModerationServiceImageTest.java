package com.aurachat.module.moderation.service;

import com.aurachat.module.moderation.dto.ImageModerationResult;
import com.aurachat.module.moderation.entity.ModerationFlag;
import com.aurachat.module.moderation.repository.ModerationFlagRepository;
import com.aurachat.module.moderation.repository.ModerationKeywordRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ContentModerationServiceImageTest {

    @Mock ModerationKeywordRepository keywordRepository;
    @Mock ModerationFlagRepository flagRepository;
    @Mock SightengineImageModerationService sightengineImageModerationService;
    @InjectMocks ContentModerationService contentModerationService;

    @BeforeEach
    void setUp() {
        when(sightengineImageModerationService.isConfigured()).thenReturn(true);
        when(flagRepository.existsByContentTypeAndContentIdAndStatus(any(), any(), eq("PENDING"))).thenReturn(false);
        when(flagRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void flagUploadedImageIfNeeded_createsSensitiveImageFlag() {
        when(sightengineImageModerationService.checkImageUrl("https://ik.imagekit.io/test.jpg"))
            .thenReturn(new ImageModerationResult(true, List.of("sexual_activity:0.91"), "nudity"));

        contentModerationService.flagUploadedImageIfNeeded("media-1", "user-1", "https://ik.imagekit.io/test.jpg");

        ArgumentCaptor<ModerationFlag> captor = ArgumentCaptor.forClass(ModerationFlag.class);
        verify(flagRepository).save(captor.capture());
        assertThat(captor.getValue().getReason()).isEqualTo("SENSITIVE_IMAGE");
        assertThat(captor.getValue().getMatchedKeywords()).contains("sexual_activity:0.91");
    }

    @Test
    void flagUploadedImageIfNeeded_skipsSafeImage() {
        when(sightengineImageModerationService.checkImageUrl(any())).thenReturn(ImageModerationResult.safe());

        contentModerationService.flagUploadedImageIfNeeded("media-1", "user-1", "https://ik.imagekit.io/safe.jpg");

        verify(flagRepository, never()).save(any());
    }
}
