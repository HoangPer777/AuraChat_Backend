package com.aurachat.module.message.service;

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

@Service
@RequiredArgsConstructor
@Slf4j
public class GroupAvatarUploadService {

    private final ImageKitClient imageKitClient;

    private static final String GROUP_AVATAR_FOLDER = "/aurachat/group-avatars";
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024;
    private static final List<String> ALLOWED_CONTENT_TYPES = Arrays.asList(
        "image/jpeg",
        "image/jpg",
        "image/png",
        "image/gif",
        "image/webp"
    );

    public String uploadGroupAvatar(String conversationId, MultipartFile file) {
        validateFile(file);

        try {
            String extension = getFileExtension(file.getOriginalFilename());
            String uniqueFilename = String.format(
                "group_%s_%s%s",
                conversationId,
                UUID.randomUUID().toString().substring(0, 8),
                extension
            );

            FileUploadParams uploadParams = FileUploadParams.builder()
                .file(file.getBytes())
                .fileName(uniqueFilename)
                .folder(GROUP_AVATAR_FOLDER)
                .build();

            log.info("Uploading group avatar for conversation {}", conversationId);
            FileUploadResponse uploadResult = imageKitClient.files().upload(uploadParams);

            return uploadResult.url()
                .orElseThrow(() -> new SystemException(
                    ErrorCode.MEDIA_UPLOAD_FAILED,
                    "ImageKit",
                    "ImageKit did not return a URL for the uploaded file"
                ));
        } catch (IOException e) {
            throw new SystemException(
                ErrorCode.MEDIA_UPLOAD_FAILED,
                "GroupAvatarUploadService",
                "Failed to read uploaded file",
                e
            );
        } catch (Exception e) {
            throw new SystemException(
                ErrorCode.MEDIA_UPLOAD_FAILED,
                "ImageKit",
                "Failed to upload group avatar to ImageKit",
                e
            );
        }
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ValidationException(
                ErrorCode.VALIDATION_REQUIRED_FIELD,
                "file",
                null,
                "Avatar file is required"
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
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
            throw new ValidationException(
                ErrorCode.MEDIA_INVALID_TYPE,
                "file",
                contentType,
                "Invalid file type. Allowed types: JPEG, PNG, GIF, WebP"
            );
        }
    }

    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf("."));
    }
}
