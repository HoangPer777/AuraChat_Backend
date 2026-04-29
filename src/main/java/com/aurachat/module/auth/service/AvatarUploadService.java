package com.aurachat.module.auth.service;

import com.aurachat.common.exception.BusinessLogicException;
import com.aurachat.common.exception.ErrorCode;
import com.aurachat.common.exception.SystemException;
import com.aurachat.common.exception.ValidationException;
import io.imagekit.client.ImageKitClient;
import io.imagekit.models.files.FileUploadParams;
import io.imagekit.models.files.FileUploadResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * Service for handling avatar upload to ImageKit.
 * Uploads images to ImageKit in the Home/aurachat/avatar folder.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AvatarUploadService {

    private final ImageKitClient imageKit;
    private final AuthService authService;

    private static final String AVATAR_FOLDER = "Home/aurachat/avatar";
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB
    private static final List<String> ALLOWED_CONTENT_TYPES = Arrays.asList(
        "image/jpeg",
        "image/jpg",
        "image/png",
        "image/gif",
        "image/webp"
    );

    /**
     * Uploads avatar image to ImageKit and updates user's avatar URL.
     *
     * @param userId the ID of the user uploading the avatar
     * @param file the avatar image file
     * @return the URL of the uploaded avatar
     */
    public String uploadAvatar(String userId, MultipartFile file) {
        // Validate file
        validateFile(file);

        try {
            // Generate unique filename
            String originalFilename = file.getOriginalFilename();
            String extension = getFileExtension(originalFilename);
            String uniqueFilename = String.format("avatar_%s_%s%s", 
                userId, 
                UUID.randomUUID().toString().substring(0, 8),
                extension
            );

            // Prepare upload request using new SDK API
            FileUploadParams uploadParams = FileUploadParams.builder()
                .file(file.getBytes())
                .fileName(uniqueFilename)
                .folder(AVATAR_FOLDER)
                .useUniqueFileName(false) // We already made it unique
                .build();

            // Upload to ImageKit
            log.info("Uploading avatar for user {} to ImageKit", userId);
            FileUploadResponse uploadResult = imageKit.files().upload(uploadParams);

            // Get the URL from the result
            String avatarUrl = uploadResult.url()
                .orElseThrow(() -> new SystemException(
                    ErrorCode.MEDIA_UPLOAD_FAILED,
                    "ImageKit",
                    "ImageKit did not return a URL for the uploaded file"
                ));
            log.info("Avatar uploaded successfully for user {}: {}", userId, avatarUrl);

            // Update user's avatar URL in database
            authService.updateAvatarUrl(userId, avatarUrl);

            return avatarUrl;

        } catch (IOException e) {
            log.error("Failed to read file for user {}: {}", userId, e.getMessage(), e);
            throw new SystemException(
                ErrorCode.MEDIA_UPLOAD_FAILED,
                "AvatarUploadService",
                "Failed to read uploaded file",
                e
            );
        } catch (Exception e) {
            log.error("Failed to upload avatar to ImageKit for user {}: {}", userId, e.getMessage(), e);
            throw new SystemException(
                ErrorCode.MEDIA_UPLOAD_FAILED,
                "ImageKit",
                "Failed to upload avatar to ImageKit",
                e
            );
        }
    }

    /**
     * Validates the uploaded file.
     *
     * @param file the file to validate
     * @throws ValidationException if validation fails
     */
    private void validateFile(MultipartFile file) {
        // Check if file is empty
        if (file == null || file.isEmpty()) {
            throw new ValidationException(
                ErrorCode.VALIDATION_REQUIRED_FIELD,
                "file",
                null,
                "Avatar file is required"
            );
        }

        // Check file size
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new ValidationException(
                ErrorCode.MEDIA_SIZE_EXCEEDED,
                "file",
                file.getSize(),
                String.format("File size exceeds maximum allowed size of %d MB", MAX_FILE_SIZE / (1024 * 1024))
            );
        }

        // Check content type
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
            throw new ValidationException(
                ErrorCode.MEDIA_INVALID_TYPE,
                "file",
                contentType,
                "Invalid file type. Allowed types: JPEG, PNG, GIF, WebP"
            );
        }
    }

    /**
     * Extracts file extension from filename.
     *
     * @param filename the filename
     * @return the file extension including the dot (e.g., ".jpg")
     */
    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf("."));
    }
}
