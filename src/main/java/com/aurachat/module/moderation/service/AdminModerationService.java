package com.aurachat.module.moderation.service;

import com.aurachat.common.exception.BusinessLogicException;
import com.aurachat.common.exception.ErrorCode;
import com.aurachat.module.admin.dto.PageResponse;
import com.aurachat.module.admin.service.AdminMediaService;
import com.aurachat.module.admin.service.AdminPostService;
import com.aurachat.module.auth.entity.User;
import com.aurachat.module.auth.repository.UserRepository;
import com.aurachat.module.media.entity.Media;
import com.aurachat.module.moderation.dto.*;
import com.aurachat.module.moderation.entity.ModerationFlag;
import com.aurachat.module.moderation.entity.ModerationKeyword;
import com.aurachat.module.moderation.repository.ModerationFlagRepository;
import com.aurachat.module.moderation.repository.ModerationKeywordRepository;
import com.aurachat.module.notification.service.PushNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminModerationService {

    private final MongoTemplate mongoTemplate;
    private final ModerationFlagRepository flagRepository;
    private final ModerationKeywordRepository keywordRepository;
    private final UserRepository userRepository;
    private final AdminPostService adminPostService;
    private final AdminMediaService adminMediaService;
    private final ContentModerationService contentModerationService;
    private final PushNotificationService pushNotificationService;

    public PageResponse<ModerationFlagDto> getFlags(Pageable pageable, String status, String contentType) {
        Query query = buildFlagQuery(status, contentType);
        long total = mongoTemplate.count(query, ModerationFlag.class);
        query.with(pageable);
        List<ModerationFlag> flags = mongoTemplate.find(query, ModerationFlag.class);
        Map<String, User> authors = loadAuthors(flags);
        List<ModerationFlagDto> content = flags.stream()
            .map(flag -> ModerationFlagDto.from(flag, authors.get(flag.getAuthorId())))
            .toList();
        return PageResponse.of(content, pageable.getPageNumber(), pageable.getPageSize(), total);
    }

    public ModerationFlagStatsResponse getStats() {
        return new ModerationFlagStatsResponse(
            flagRepository.countByStatus("PENDING"),
            flagRepository.countByStatusAndContentType("PENDING", "POST"),
            flagRepository.countByStatusAndContentType("PENDING", "COMMENT"),
            flagRepository.countByStatusAndContentType("PENDING", "MEDIA")
        );
    }

    public ModerationFlagDto dismissFlag(String flagId, String adminId, String note) {
        ModerationFlag flag = requirePendingFlag(flagId);
        return completeReview(flag, adminId, "DISMISSED", note);
    }

    public ModerationFlagDto removeContent(String flagId, String adminId, String note) {
        ModerationFlag flag = requirePendingFlag(flagId);
        deleteFlaggedContent(flag, adminId);
        return completeReview(flag, adminId, "REMOVED", note);
    }

    public ModerationFlagDto warnUser(String flagId, String adminId, String message) {
        ModerationFlag flag = requirePendingFlag(flagId);
        User user = userRepository.findById(flag.getAuthorId())
            .orElseThrow(() -> new BusinessLogicException(ErrorCode.USER_NOT_FOUND, "User not found"));
        user.setWarningCount(user.getWarningCount() + 1);
        user.setUpdatedAt(Instant.now());
        userRepository.save(user);
        pushNotificationService.notifyModerationWarning(flag.getAuthorId(), message);
        log.info("Admin action=WARN_USER adminId={} userId={} flagId={}", adminId, user.getId(), flagId);
        return completeReview(flag, adminId, "WARNED", message);
    }

    public ModerationFlagDto flagMedia(String mediaId, String adminId, String note) {
        Media media = mongoTemplate.findById(mediaId, Media.class);
        if (media == null || media.isDeleted()) {
            throw new BusinessLogicException(ErrorCode.MEDIA_NOT_FOUND, "Media not found");
        }
        ModerationFlag flag = contentModerationService.flagMediaManually(
            mediaId, media.getOwnerId(), media.getUrl(), note, adminId
        );
        User author = userRepository.findById(flag.getAuthorId()).orElse(null);
        return ModerationFlagDto.from(flag, author);
    }

    public List<ModerationKeywordDto> getKeywords() {
        return keywordRepository.findAll().stream()
            .map(ModerationKeywordDto::from)
            .toList();
    }

    public ModerationKeywordDto addKeyword(String word) {
        String normalized = word == null ? "" : word.trim().toLowerCase();
        if (normalized.isBlank()) {
            throw new BusinessLogicException(ErrorCode.VALIDATION_REQUIRED_FIELD, "word");
        }
        if (keywordRepository.existsByWordIgnoreCase(normalized)) {
            throw new BusinessLogicException(ErrorCode.MODERATION_KEYWORD_EXISTS, "Keyword already exists");
        }
        ModerationKeyword saved = keywordRepository.save(ModerationKeyword.builder()
            .word(normalized)
            .enabled(true)
            .createdAt(Instant.now())
            .build());
        return ModerationKeywordDto.from(saved);
    }

    public void deleteKeyword(String keywordId) {
        ModerationKeyword keyword = keywordRepository.findById(keywordId)
            .orElseThrow(() -> new BusinessLogicException(ErrorCode.MODERATION_KEYWORD_NOT_FOUND, "Keyword not found"));
        keywordRepository.delete(keyword);
    }

    private ModerationFlagDto completeReview(ModerationFlag flag, String adminId, String status, String note) {
        flag.setStatus(status);
        flag.setReviewedBy(adminId);
        flag.setReviewedAt(Instant.now());
        if (note != null && !note.isBlank()) {
            flag.setAdminNote(note.trim());
        }
        ModerationFlag saved = flagRepository.save(flag);
        User author = userRepository.findById(saved.getAuthorId()).orElse(null);
        log.info("Admin action=REVIEW_FLAG adminId={} flagId={} status={}", adminId, saved.getId(), status);
        return ModerationFlagDto.from(saved, author);
    }

    private void deleteFlaggedContent(ModerationFlag flag, String adminId) {
        switch (flag.getContentType()) {
            case "POST" -> adminPostService.deletePost(flag.getContentId(), adminId);
            case "COMMENT" -> adminPostService.deleteComment(flag.getContentId(), adminId);
            case "MEDIA" -> adminMediaService.deleteMedia(flag.getContentId(), adminId);
            default -> throw new BusinessLogicException(ErrorCode.MODERATION_FLAG_NOT_FOUND, "Unknown content type");
        }
    }

    private ModerationFlag requirePendingFlag(String flagId) {
        ModerationFlag flag = flagRepository.findById(flagId)
            .orElseThrow(() -> new BusinessLogicException(ErrorCode.MODERATION_FLAG_NOT_FOUND, "Flag not found"));
        if (!"PENDING".equals(flag.getStatus())) {
            throw new BusinessLogicException(ErrorCode.MODERATION_FLAG_ALREADY_REVIEWED, "Flag already reviewed");
        }
        return flag;
    }

    private Query buildFlagQuery(String status, String contentType) {
        List<Criteria> filters = new ArrayList<>();
        filters.add(Criteria.where("status").is(status == null || status.isBlank() ? "PENDING" : status.toUpperCase()));
        if (contentType != null && !contentType.isBlank()) {
            filters.add(Criteria.where("contentType").is(contentType.toUpperCase()));
        }
        return Query.query(new Criteria().andOperator(filters.toArray(Criteria[]::new)));
    }

    private Map<String, User> loadAuthors(List<ModerationFlag> flags) {
        List<String> authorIds = flags.stream().map(ModerationFlag::getAuthorId).distinct().toList();
        if (authorIds.isEmpty()) {
            return Map.of();
        }
        return userRepository.findAllById(authorIds).stream()
            .collect(Collectors.toMap(User::getId, user -> user));
    }
}
