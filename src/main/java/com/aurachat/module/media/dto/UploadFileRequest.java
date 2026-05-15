package com.aurachat.module.media.dto;

import jakarta.validation.constraints.NotNull;
import org.springframework.web.multipart.MultipartFile;

public record UploadFileRequest(
    @NotNull
    MultipartFile file
) {}
