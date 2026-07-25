package com.socialmedia.analytics.dto.response;

import java.time.LocalDate;

public record DauResponse(LocalDate date, int activeUsers) {
}
