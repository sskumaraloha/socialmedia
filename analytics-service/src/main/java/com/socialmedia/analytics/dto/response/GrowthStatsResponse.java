package com.socialmedia.analytics.dto.response;

import java.time.LocalDate;
import java.util.List;

public record GrowthStatsResponse(LocalDate from, LocalDate to, long totalNewUsers, List<DailyCount> dailyBreakdown) {
}
