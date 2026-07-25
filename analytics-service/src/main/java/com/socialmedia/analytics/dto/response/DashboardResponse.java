package com.socialmedia.analytics.dto.response;

import java.time.LocalDate;

public record DashboardResponse(LocalDate date, int dau, int mau, long newSignupsToday, long messagesToday,
        long storageBytesToday, long revenueCentsToday) {
}
