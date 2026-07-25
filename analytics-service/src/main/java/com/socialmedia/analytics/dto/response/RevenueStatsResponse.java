package com.socialmedia.analytics.dto.response;

import java.time.LocalDate;
import java.util.List;

public record RevenueStatsResponse(LocalDate from, LocalDate to, long totalRevenueCents, List<DailyCount> dailyBreakdown) {
}
