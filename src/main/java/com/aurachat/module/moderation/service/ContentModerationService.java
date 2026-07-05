package com.aurachat.module.moderation.service;

import com.aurachat.module.moderation.entity.ModerationFlag;
import com.aurachat.module.moderation.entity.ModerationKeyword;
import com.aurachat.module.moderation.repository.ModerationFlagRepository;
import com.aurachat.module.moderation.repository.ModerationKeywordRepository;
import com.aurachat.module.moderation.dto.ImageModerationResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Slf4j
@Service
@RequiredArgsConstructor
public class ContentModerationService {

    private final ModerationKeywordRepository keywordRepository;
    private final ModerationFlagRepository flagRepository;
    private final SightengineImageModerationService sightengineImageModerationService;

    public List<String> scanText(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }
        String normalizedText = normalizeForMatch(text);
        List<String> matches = new ArrayList<>();
        for (ModerationKeyword keyword : keywordRepository.findByEnabledTrue()) {
            String normalizedWord = normalizeForMatch(keyword.getWord());
            if (!normalizedWord.isBlank() && normalizedText.contains(normalizedWord)) {
                matches.add(keyword.getWord());
            }
        }
        return matches;
    }

    public void flagPostIfNeeded(String postId, String authorId, String content) {
        flagTextContentIfNeeded("POST", postId, authorId, truncate(content, 500), content);
    }

    public void flagCommentIfNeeded(String commentId, String authorId, String content) {
        flagTextContentIfNeeded("COMMENT", commentId, authorId, truncate(content, 500), content);
    }

    public ModerationFlag flagMediaManually(String mediaId, String ownerId, String previewUrl, String note, String adminId) {
        if (flagRepository.existsByContentTypeAndContentIdAndStatus("MEDIA", mediaId, "PENDING")) {
            return flagRepository.findFirstByContentTypeAndContentIdAndStatusOrderByCreatedAtDesc(
                "MEDIA", mediaId, "PENDING"
            ).orElseThrow();
        }
        ModerationFlag flag = ModerationFlag.builder()
            .contentType("MEDIA")
            .contentId(mediaId)
            .authorId(ownerId)
            .preview(previewUrl)
            .matchedKeywords(List.of())
            .reason("MANUAL")
            .status("PENDING")
            .adminNote(note)
            .createdAt(Instant.now())
            .build();
        ModerationFlag saved = flagRepository.save(flag);
        log.info("Moderation manual flag for media {} by admin {}", mediaId, adminId);
        return saved;
    }

    public void flagUploadedImageIfNeeded(String mediaId, String ownerId, String imageUrl) {
        if (!sightengineImageModerationService.isConfigured()) {
            return;
        }
        ImageModerationResult result = sightengineImageModerationService.checkImageUrl(imageUrl);
        if (!result.sensitive()) {
            return;
        }
        if (flagRepository.existsByContentTypeAndContentIdAndStatus("MEDIA", mediaId, "PENDING")) {
            return;
        }
        ModerationFlag flag = ModerationFlag.builder()
            .contentType("MEDIA")
            .contentId(mediaId)
            .authorId(ownerId)
            .preview(imageUrl)
            .matchedKeywords(result.labels())
            .reason("SENSITIVE_IMAGE")
            .status("PENDING")
            .adminNote(result.summary())
            .createdAt(Instant.now())
            .build();
        flagRepository.save(flag);
        log.info("Moderation image flag created mediaId={} labels={}", mediaId, result.labels());
    }

    private void flagTextContentIfNeeded(
        String contentType,
        String contentId,
        String authorId,
        String preview,
        String fullText
    ) {
        List<String> matches = scanText(fullText);
        if (matches.isEmpty()) {
            return;
        }
        if (flagRepository.existsByContentTypeAndContentIdAndStatus(contentType, contentId, "PENDING")) {
            return;
        }
        ModerationFlag flag = ModerationFlag.builder()
            .contentType(contentType)
            .contentId(contentId)
            .authorId(authorId)
            .preview(preview)
            .matchedKeywords(matches)
            .reason("SENSITIVE_TEXT")
            .status("PENDING")
            .createdAt(Instant.now())
            .build();
        flagRepository.save(flag);
        log.info("Moderation flag created type={} id={} keywords={}", contentType, contentId, matches);
    }

    static String normalizeForMatch(String text) {
        if (text == null) {
            return "";
        }
        String normalized = Normalizer.normalize(text, Normalizer.Form.NFD)
            .replaceAll("\\p{M}", "")
            .replace('đ', 'd')
            .replace('Đ', 'D');
        return normalized.toLowerCase(Locale.ROOT);
    }

    private String truncate(String value, int max) {
        if (value == null) {
            return "";
        }
        return value.length() <= max ? value : value.substring(0, max);
    }
}
