package com.aurachat.module.post.dto;

import java.util.List;

public record PostPageResponse(
    List<PostResponse> content,
    int page,
    int size,
    long totalElements,
    int totalPages,
    boolean first,
    boolean last
) {
    public static PostPageResponse of(List<PostResponse> content, int page, int size, long totalElements) {
        int totalPages = size == 0 ? 0 : (int) Math.ceil((double) totalElements / size);
        return new PostPageResponse(
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
