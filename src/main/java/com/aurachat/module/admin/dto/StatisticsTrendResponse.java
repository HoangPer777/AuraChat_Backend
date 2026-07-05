package com.aurachat.module.admin.dto;

import java.util.List;

public record StatisticsTrendResponse(
    List<DailyTrendPoint> points
) {}
