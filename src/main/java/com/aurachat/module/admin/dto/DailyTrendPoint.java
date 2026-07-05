package com.aurachat.module.admin.dto;

public record DailyTrendPoint(
    String date,
    long messageVolume,
    long newUsersCount,
    long dailyActiveUsers
) {}
