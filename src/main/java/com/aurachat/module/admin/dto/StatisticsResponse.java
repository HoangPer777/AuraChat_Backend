package com.aurachat.module.admin.dto;

import java.time.Instant;

public record StatisticsResponse(
    Instant startDate,
    Instant endDate,
    long dailyActiveUsers,
    long messageVolume,
    long newUsersCount,
    long onlineUsersCount,
    long totalPostsCount,
    long totalMediaCount,
    long totalMediaBytes,
    Instant generatedAt
) {}
