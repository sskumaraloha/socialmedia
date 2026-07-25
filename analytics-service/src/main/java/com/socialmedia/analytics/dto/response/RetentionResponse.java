package com.socialmedia.analytics.dto.response;

import java.time.LocalDate;

public record RetentionResponse(LocalDate cohortWeekStart, int weeksLater, long cohortSize, long retainedCount,
        double retentionRate) {
}
