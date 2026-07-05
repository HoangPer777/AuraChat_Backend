package com.aurachat.module.moderation.service;

import com.aurachat.module.moderation.entity.ModerationKeyword;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ContentModerationServiceTest {

    @Mock ModerationKeywordRepository keywordRepository;
    @Mock ModerationFlagRepository flagRepository;
    @Mock SightengineImageModerationService sightengineImageModerationService;
    @InjectMocks ContentModerationService contentModerationService;

    @BeforeEach
    void setUp() {
        when(keywordRepository.findByEnabledTrue()).thenReturn(List.of(
            keyword("lừa đảo"),
            keyword("scam"),
            keyword("đm")
        ));
        when(flagRepository.existsByContentTypeAndContentIdAndStatus(any(), any(), eq("PENDING"))).thenReturn(false);
    }

    @Test
    void scanText_findsNormalizedVietnameseKeyword() {
        List<String> matches = contentModerationService.scanText("Đây là tin lừa đảo nhé");

        assertThat(matches).contains("lừa đảo");
    }

    @Test
    void scanText_findsKeywordWithoutDiacritics() {
        List<String> matches = contentModerationService.scanText("dm may di");

        assertThat(matches).contains("đm");
    }

    @Test
    void scanText_returnsEmptyWhenClean() {
        assertThat(contentModerationService.scanText("Xin chào mọi người")).isEmpty();
    }

    @Test
    void flagPostIfNeeded_createsFlagWhenMatched() {
        when(flagRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        contentModerationService.flagPostIfNeeded("post-1", "user-1", "Nội dung scam rõ ràng");

        ArgumentCaptor<com.aurachat.module.moderation.entity.ModerationFlag> captor =
            ArgumentCaptor.forClass(com.aurachat.module.moderation.entity.ModerationFlag.class);
        verify(flagRepository).save(captor.capture());
        assertThat(captor.getValue().getContentType()).isEqualTo("POST");
        assertThat(captor.getValue().getMatchedKeywords()).contains("scam");
        assertThat(captor.getValue().getStatus()).isEqualTo("PENDING");
    }

    @Test
    void flagPostIfNeeded_skipsWhenNoMatch() {
        contentModerationService.flagPostIfNeeded("post-1", "user-1", "Bài viết bình thường");
        verify(flagRepository, never()).save(any());
    }

    private ModerationKeyword keyword(String word) {
        return ModerationKeyword.builder().word(word).enabled(true).build();
    }
}
