package com.aurachat.module.media.service;

import com.aurachat.common.exception.ErrorCode;
import com.aurachat.common.exception.SystemException;
import com.aurachat.common.exception.ValidationException;
import com.aurachat.module.media.dto.MediaResponse;
import io.imagekit.client.ImageKitClient;
import io.imagekit.models.files.FileUploadParams;
import io.imagekit.models.files.FileUploadResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Duration;
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

    private static final Set<String> BLOCKED_EXTENSIONS = Set.of(
        "exe",
        "sh",
        "bat"
    );

    private final ImageKitClient imageKit;
    private final StringRedisTemplate redisTemplate;

    public MediaResponse uploadImage(MultipartFile file, String userId) {
        enforceUploadRateLimit(userId);
        return uploadMedia(file, userId, IMAGE_FOLDER, ALLOWED_IMAGE_TYPES, ALLOWED_IMAGE_EXTENSIONS);
    }

    public MediaResponse uploadFile(MultipartFile file, String userId) {
        enforceUploadRateLimit(userId);
        return uploadMedia(file, userId, FILE_FOLDER, ALLOWED_FILE_TYPES, ALLOWED_FILE_EXTENSIONS);
    }

    private MediaResponse uploadMedia(
        MultipartFile file,
        String userId,
        String folder,
        Set<String> allowedTypes,
        Set<String> allowedExtensions
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

            return new MediaResponse(
                url,
                uniqueName,
                originalName,
                file.getContentType(),
                file.getSize(),
                "ImageKit"
            );
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
        if (contentType == null || !allowedTypes.contains(contentType.toLowerCase())) {
            throw new ValidationException(
                ErrorCode.MEDIA_INVALID_TYPE,
                "file",
                contentType,
                "Invalid file content type"
            );
        }

        String extension = getFileExtensionLower(file.getOriginalFilename());
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

    private String getFileExtensionLower(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
    }
}
