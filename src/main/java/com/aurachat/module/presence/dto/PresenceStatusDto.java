package com.aurachat.module.presence.dto;

import java.time.Instant;

public record PresenceStatusDto(String userId, String status, Instant lastSeen) {}
