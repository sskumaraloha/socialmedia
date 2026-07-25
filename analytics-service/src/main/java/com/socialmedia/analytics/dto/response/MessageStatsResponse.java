package com.socialmedia.analytics.dto.response;

import java.time.LocalDate;
import java.util.List;

public record MessageStatsResponse(LocalDate from, LocalDate to, long totalMessages, List<DailyCount> dailyBreakdown) {
}
