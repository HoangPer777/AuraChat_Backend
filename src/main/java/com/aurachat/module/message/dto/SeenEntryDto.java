package com.aurachat.module.message.dto;

import java.time.Instant;

public record SeenEntryDto(String userId, Instant seenAt) {}
