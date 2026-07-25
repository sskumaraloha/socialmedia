package com.socialmedia.analytics.dto.response;

import java.time.LocalDate;
import java.util.List;

public record StorageStatsResponse(LocalDate from, LocalDate to, long totalBytesUploaded, List<DailyCount> dailyBreakdown) {
}
