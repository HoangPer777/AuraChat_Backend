package com.aurachat.module.media.dto;

import java.util.List;

public record MediaPageResponse(
    List<MediaResponse> content,
    int page,
    int size,
    long totalElements,
    int totalPages,
    boolean first,
    boolean last
) {
    public static MediaPageResponse of(List<MediaResponse> content, int page, int size, long totalElements) {
        int totalPages = size == 0 ? 0 : (int) Math.ceil((double) totalElements / size);
        return new MediaPageResponse(
            content,
            page,
            size,
            totalElements,
            totalPages,
            page == 0,
            totalPages == 0 || page >= totalPages - 1
        );
    }
}
