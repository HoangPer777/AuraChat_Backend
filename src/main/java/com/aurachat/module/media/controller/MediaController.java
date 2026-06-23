package com.aurachat.module.media.controller;

import com.aurachat.common.response.DataResponse;
import com.aurachat.module.media.dto.MediaPageResponse;
import com.aurachat.module.media.dto.MediaResponse;
import com.aurachat.module.media.dto.UploadFileRequest;
import com.aurachat.module.media.dto.UploadImageRequest;
import com.aurachat.module.media.service.MediaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/media")
@RequiredArgsConstructor
public class MediaController {

    private final MediaService mediaService;

    @PostMapping(value = "/upload/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<DataResponse<MediaResponse>> uploadImage(
        @AuthenticationPrincipal String userId,
        @Valid @ModelAttribute UploadImageRequest request
    ) {
        MediaResponse response = mediaService.uploadImage(request.file(), userId);
        return ResponseEntity.ok(DataResponse.success(response, "Image uploaded successfully"));
    }

    @PostMapping(value = "/upload/file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<DataResponse<MediaResponse>> uploadFile(
        @AuthenticationPrincipal String userId,
        @Valid @ModelAttribute UploadFileRequest request
    ) {
        MediaResponse response = mediaService.uploadFile(request.file(), userId);
        return ResponseEntity.ok(DataResponse.success(response, "File uploaded successfully"));
    }

    @PostMapping(value = "/upload/audio", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<DataResponse<MediaResponse>> uploadAudio(
        @AuthenticationPrincipal String userId,
        @Valid @ModelAttribute UploadFileRequest request
    ) {
        MediaResponse response = mediaService.uploadAudio(request.file(), userId);
        return ResponseEntity.ok(DataResponse.success(response, "Audio uploaded successfully"));
    }

    @GetMapping
    public ResponseEntity<DataResponse<MediaPageResponse>> getUserMedia(
        @AuthenticationPrincipal String userId,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        MediaPageResponse response = mediaService.getUserMedia(userId, page, size);
        return ResponseEntity.ok(DataResponse.success(response));
    }

    @GetMapping("/{mediaId}")
    public ResponseEntity<DataResponse<MediaResponse>> getMediaDetail(
        @AuthenticationPrincipal String userId,
        @PathVariable String mediaId
    ) {
        MediaResponse response = mediaService.getMediaDetail(userId, mediaId);
        return ResponseEntity.ok(DataResponse.success(response));
    }

    @DeleteMapping("/{mediaId}")
    public ResponseEntity<DataResponse<Void>> deleteMedia(
        @AuthenticationPrincipal String userId,
        @PathVariable String mediaId
    ) {
        mediaService.deleteMedia(userId, mediaId);
        return ResponseEntity.ok(DataResponse.success("Media deleted successfully"));
    }
}
