package com.aurachat.module.message.dto;

import java.time.Instant;

public record LastMessageDto(String content, String senderId, Instant sentAt, String type) {}
