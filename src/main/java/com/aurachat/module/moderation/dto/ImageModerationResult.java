package com.aurachat.module.moderation.dto;

import java.util.List;

public record ImageModerationResult(
    boolean sensitive,
    List<String> labels,
    String summary
) {
    public static ImageModerationResult safe() {
        return new ImageModerationResult(false, List.of(), "safe");
    }
}
