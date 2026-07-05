package com.aurachat.module.media.service;

import com.aurachat.common.exception.ErrorCode;
import com.aurachat.common.exception.AuthorizationException;
import com.aurachat.common.exception.BusinessLogicException;
import com.aurachat.common.exception.SystemException;
import com.aurachat.common.exception.ValidationException;
import com.aurachat.module.media.dto.MediaPageResponse;
import com.aurachat.module.media.dto.MediaResponse;
import com.aurachat.module.media.entity.Media;
import com.aurachat.module.media.repository.MediaRepository;
import io.imagekit.client.ImageKitClient;
import io.imagekit.models.files.FileUploadParams;
import io.imagekit.models.files.FileUploadResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Instant;
import java.time.Duration;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class MediaService {

    private static final String IMAGE_FOLDER = "Home/aurachat/media/images";
    private static final String FILE_FOLDER = "Home/aurachat/media/files";
    private static final long MAX_FILE_SIZE = 10L * 1024 * 1024; // 10MB
    private static final int UPLOAD_LIMIT_PER_HOUR = 10;
    private static final Duration UPLOAD_WINDOW = Duration.ofHours(1);

    private static final Set<String> ALLOWED_IMAGE_TYPES = Set.of(
        "image/jpeg",
        "image/jpg",
        "image/png",
        "image/gif",
        "image/webp"
    );

    private static final Set<String> ALLOWED_IMAGE_EXTENSIONS = Set.of(
        "jpg",
        "jpeg",
        "png",
        "gif",
        "webp"
    );

    private static final Set<String> ALLOWED_FILE_TYPES = Set.of(
        "application/pdf",
        "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
        "text/plain"
    );

    private static final Set<String> ALLOWED_FILE_EXTENSIONS = Set.of(
        "pdf",
        "docx",
        "xlsx",
        "txt"
    );

    private static final Set<String> ALLOWED_AUDIO_TYPES = Set.of(
        "audio/webm",
        "audio/ogg",
        "audio/mpeg",
        "audio/mp4",
        "audio/mp3",
        "audio/wav",
        "audio/x-wav",
        "audio/aac",
        "audio/x-m4a",
        "audio/m4a"
    );

    private static final Set<String> ALLOWED_AUDIO_EXTENSIONS = Set.of(
        "webm",
        "ogg",
        "mp3",
        "m4a",
        "wav"
    );

    private static final Set<String> BLOCKED_EXTENSIONS = Set.of(
        "exe",
        "sh",
        "bat"
    );

    private final ImageKitClient imageKit;
    private final StringRedisTemplate redisTemplate;
    private final MediaRepository mediaRepository;

    public MediaResponse uploadImage(MultipartFile file, String userId) {
        enforceUploadRateLimit(userId);
        return uploadMedia(file, userId, IMAGE_FOLDER, ALLOWED_IMAGE_TYPES, ALLOWED_IMAGE_EXTENSIONS, "IMAGE");
    }

    public MediaResponse uploadFile(MultipartFile file, String userId) {
        enforceUploadRateLimit(userId);
        return uploadMedia(file, userId, FILE_FOLDER, ALLOWED_FILE_TYPES, ALLOWED_FILE_EXTENSIONS, "FILE");
    }

    public MediaResponse uploadAudio(MultipartFile file, String userId) {
        enforceUploadRateLimit(userId);
        return uploadMedia(file, userId, FILE_FOLDER, ALLOWED_AUDIO_TYPES, ALLOWED_AUDIO_EXTENSIONS, "AUDIO");
    }

    public MediaPageResponse getUserMedia(String userId, int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 50);
        Pageable pageable = PageRequest.of(safePage, safeSize);
        Page<Media> result = mediaRepository.findByOwnerIdAndDeletedFalseOrderByCreatedAtDesc(userId, pageable);
        return MediaPageResponse.of(
            result.getContent().stream().map(this::toResponse).toList(),
            safePage,
            safeSize,
            result.getTotalElements()
        );
    }

    public MediaResponse getMediaDetail(String userId, String mediaId) {
        Media media = findExistingMedia(mediaId);
        ensureOwnership(userId, media);
        return toResponse(media);
    }

    public void deleteMedia(String userId, String mediaId) {
        Media media = findExistingMedia(mediaId);
        ensureOwnership(userId, media);
        performDelete(media);
    }

    public void adminDeleteMedia(String mediaId, String adminId) {
        Media media = findExistingMedia(mediaId);
        performDelete(media);
        log.info("Admin action=DELETE_MEDIA adminId={} mediaId={} ownerId={}", adminId, mediaId, media.getOwnerId());
    }

    private void performDelete(Media media) {
        try {
            if (media.getFileId() != null && !media.getFileId().isBlank()) {
                imageKit.files().delete(media.getFileId());
            }
        } catch (Exception e) {
            log.error("Failed to delete media {} from ImageKit", media.getId(), e);
            throw new SystemException(
                ErrorCode.MEDIA_DELETE_FAILED,
                "ImageKit",
                "Failed to delete media from ImageKit",
                e
            );
        }

        media.setDeleted(true);
        media.setDeletedAt(Instant.now());
        mediaRepository.save(media);
    }

    private MediaResponse uploadMedia(
        MultipartFile file,
        String userId,
        String folder,
        Set<String> allowedTypes,
        Set<String> allowedExtensions,
        String mediaType
    ) {
        validateFile(file, allowedTypes, allowedExtensions);

        String originalName = file.getOriginalFilename();
        String extension = getFileExtensionLower(originalName);
        String uniqueName = String.format("media_%s_%s.%s",
            userId,
            UUID.randomUUID().toString().substring(0, 8),
            extension
        );

        try {
            FileUploadParams params = FileUploadParams.builder()
                .file(file.getBytes())
                .fileName(uniqueName)
                .folder(folder)
                .useUniqueFileName(false)
                .build();

            log.info("Uploading media for user {} to ImageKit: {}", userId, uniqueName);
            FileUploadResponse uploadResult = imageKit.files().upload(params);

            String url = uploadResult.url()
                .orElseThrow(() -> new SystemException(
                    ErrorCode.MEDIA_UPLOAD_FAILED,
                    "ImageKit",
                    "ImageKit did not return a URL for the uploaded file"
                ));
            String fileId = extractFileId(uploadResult);

            Media media = mediaRepository.save(Media.builder()
                .ownerId(userId)
                .fileId(fileId)
                .url(url)
                .fileName(uniqueName)
                .originalFileName(originalName)
                .contentType(file.getContentType())
                .size(file.getSize())
                .provider("ImageKit")
                .mediaType(mediaType)
                .createdAt(Instant.now())
                .build());
            return toResponse(media);
        } catch (IOException e) {
            log.error("Failed to read media file for user {}: {}", userId, e.getMessage(), e);
            throw new SystemException(
                ErrorCode.MEDIA_UPLOAD_FAILED,
                "MediaService",
                "Failed to read uploaded file",
                e
            );
        } catch (Exception e) {
            log.error("Failed to upload media to ImageKit for user {}: {}", userId, e.getMessage(), e);
            throw new SystemException(
                ErrorCode.MEDIA_UPLOAD_FAILED,
                "ImageKit",
                "Failed to upload media to ImageKit",
                e
            );
        }
    }

    private void validateFile(
        MultipartFile file,
        Set<String> allowedTypes,
        Set<String> allowedExtensions
    ) {
        if (file == null || file.isEmpty()) {
            throw new ValidationException(
                ErrorCode.VALIDATION_REQUIRED_FIELD,
                "file",
                null,
                "File is required"
            );
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new ValidationException(
                ErrorCode.MEDIA_SIZE_EXCEEDED,
                "file",
                file.getSize(),
                String.format("File size exceeds maximum allowed size of %d MB", MAX_FILE_SIZE / (1024 * 1024))
            );
        }

        String contentType = file.getContentType();
        String extension = getFileExtensionLower(file.getOriginalFilename());

        if (!isAllowedContentType(contentType, allowedTypes)
            && !isGenericBinaryWithAllowedExtension(contentType, extension, allowedExtensions)) {
            throw new ValidationException(
                ErrorCode.MEDIA_INVALID_TYPE,
                "file",
                contentType,
                "Invalid file content type"
            );
        }

        if (extension.isEmpty()) {
            throw new ValidationException(
                ErrorCode.MEDIA_INVALID_TYPE,
                "file",
                null,
                "File extension is required"
            );
        }

        if (BLOCKED_EXTENSIONS.contains(extension)) {
            throw new ValidationException(
                ErrorCode.MEDIA_INVALID_TYPE,
                "file",
                extension,
                "Executable files are not allowed"
            );
        }

        if (!allowedExtensions.contains(extension)) {
            throw new ValidationException(
                ErrorCode.MEDIA_INVALID_TYPE,
                "file",
                extension,
                "Invalid file extension"
            );
        }
    }

    private boolean isAllowedContentType(String contentType, Set<String> allowedTypes) {
        if (contentType == null || contentType.isBlank()) {
            return false;
        }

        String lower = contentType.toLowerCase(Locale.ROOT).trim();
        if (allowedTypes.contains(lower)) {
            return true;
        }

        String baseType = lower.split(";")[0].trim();
        if (allowedTypes.contains(baseType)) {
            return true;
        }

        return allowedTypes.stream().anyMatch(allowed -> lower.startsWith(allowed + ";"));
    }

    private boolean isGenericBinaryWithAllowedExtension(
        String contentType,
        String extension,
        Set<String> allowedExtensions
    ) {
        if (extension == null || extension.isEmpty() || !allowedExtensions.contains(extension)) {
            return false;
        }

        if (contentType == null || contentType.isBlank()) {
            return true;
        }

        String baseType = contentType.toLowerCase(Locale.ROOT).split(";")[0].trim();
        return "application/octet-stream".equals(baseType) || "binary/octet-stream".equals(baseType);
    }

    private void enforceUploadRateLimit(String userId) {
        String key = uploadRateKey(userId);
        Long count = redisTemplate.opsForValue().increment(key);
        if (count != null && count == 1L) {
            redisTemplate.expire(key, UPLOAD_WINDOW);
        }
        if (count != null && count > UPLOAD_LIMIT_PER_HOUR) {
            throw new ValidationException(
                ErrorCode.VALIDATION_FAILED,
                "rateLimit",
                count,
                "Too many uploads. Please try again later."
            );
        }
    }

    private String uploadRateKey(String userId) {
        return "media:rate:upload:" + userId;
    }

    private Media findExistingMedia(String mediaId) {
        return mediaRepository.findByIdAndDeletedFalse(mediaId)
            .orElseThrow(() -> new BusinessLogicException(
                ErrorCode.MEDIA_NOT_FOUND,
                "Media not found",
                "media/" + mediaId
            ));
    }

    private void ensureOwnership(String userId, Media media) {
        if (!media.getOwnerId().equals(userId)) {
            throw new AuthorizationException("media/" + media.getId(), "OWNER", userId);
        }
    }

    private String extractFileId(FileUploadResponse response) {
        try {
            var method = response.getClass().getMethod("fileId");
            Object value = method.invoke(response);
            if (value instanceof String strValue && !strValue.isBlank()) {
                return strValue;
            }
            if (value instanceof java.util.Optional<?> optional && optional.isPresent()) {
                Object optionalValue = optional.get();
                if (optionalValue instanceof String strValue && !strValue.isBlank()) {
                    return strValue;
                }
            }
        } catch (Exception ignored) {
            // Fallback below
        }
        return null;
    }

    private MediaResponse toResponse(Media media) {
        return new MediaResponse(
            media.getId(),
            media.getFileId(),
            media.getUrl(),
            media.getFileName(),
            media.getOriginalFileName(),
            media.getContentType(),
            media.getSize(),
            media.getProvider(),
            media.getMediaType(),
            media.getCreatedAt()
        );
    }

    private String getFileExtensionLower(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
    }
}
