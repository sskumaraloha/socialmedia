package com.socialmedia.analytics.dto.response;

import java.time.LocalDate;

public record MauResponse(LocalDate asOf, int activeUsers) {
}
