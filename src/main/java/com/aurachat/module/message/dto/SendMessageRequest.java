package com.aurachat.module.message.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record SendMessageRequest(
    String conversationId,
    String content,
    @NotNull @Pattern(regexp = "TEXT|IMAGE|FILE|CALL_LOG|VOICE|STICKER") String type,
    String fileUrl,
    String fileName,
    Long fileSize
) {}
