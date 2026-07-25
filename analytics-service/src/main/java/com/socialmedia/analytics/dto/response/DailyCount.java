package com.socialmedia.analytics.dto.response;

import java.time.LocalDate;

public record DailyCount(LocalDate date, long count) {
}
