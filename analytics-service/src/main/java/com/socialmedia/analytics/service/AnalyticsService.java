package com.socialmedia.analytics.service;

import com.socialmedia.analytics.dto.response.DashboardResponse;
import com.socialmedia.analytics.dto.response.DauResponse;
import com.socialmedia.analytics.dto.response.GrowthStatsResponse;
import com.socialmedia.analytics.dto.response.MauResponse;
import com.socialmedia.analytics.dto.response.MessageStatsResponse;
import com.socialmedia.analytics.dto.response.RetentionResponse;
import com.socialmedia.analytics.dto.response.RevenueStatsResponse;
import com.socialmedia.analytics.dto.response.StorageStatsResponse;
import java.time.LocalDate;

public interface AnalyticsService {

    DauResponse getDau(LocalDate date);

    MauResponse getMau(LocalDate asOf);

    MessageStatsResponse getMessageStats(LocalDate from, LocalDate to);

    GrowthStatsResponse getGrowthStats(LocalDate from, LocalDate to);

    StorageStatsResponse getStorageStats(LocalDate from, LocalDate to);

    RevenueStatsResponse getRevenueStats(LocalDate from, LocalDate to);

    RetentionResponse getRetention(LocalDate cohortWeekStart, int weeksLater);

    DashboardResponse getDashboard(LocalDate date);
}
