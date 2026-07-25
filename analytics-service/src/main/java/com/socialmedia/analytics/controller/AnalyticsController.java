package com.socialmedia.analytics.controller;

import com.socialmedia.analytics.dto.response.DashboardResponse;
import com.socialmedia.analytics.dto.response.DauResponse;
import com.socialmedia.analytics.dto.response.GrowthStatsResponse;
import com.socialmedia.analytics.dto.response.MauResponse;
import com.socialmedia.analytics.dto.response.MessageStatsResponse;
import com.socialmedia.analytics.dto.response.RetentionResponse;
import com.socialmedia.analytics.dto.response.RevenueStatsResponse;
import com.socialmedia.analytics.dto.response.StorageStatsResponse;
import com.socialmedia.analytics.service.AnalyticsService;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Every endpoint here is admin-only - DAU/MAU/revenue/growth are business-sensitive,
 * not something any authenticated end user should be able to pull. */
@RestController
@RequestMapping("/api/v1/analytics")
@PreAuthorize("hasRole('ADMIN')")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @GetMapping("/dau")
    public DauResponse getDau(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return analyticsService.getDau(date);
    }

    @GetMapping("/mau")
    public MauResponse getMau(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return analyticsService.getMau(date);
    }

    @GetMapping("/messages")
    public MessageStatsResponse getMessageStats(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return analyticsService.getMessageStats(from, to);
    }

    @GetMapping("/growth")
    public GrowthStatsResponse getGrowthStats(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return analyticsService.getGrowthStats(from, to);
    }

    @GetMapping("/storage")
    public StorageStatsResponse getStorageStats(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return analyticsService.getStorageStats(from, to);
    }

    @GetMapping("/revenue")
    public RevenueStatsResponse getRevenueStats(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return analyticsService.getRevenueStats(from, to);
    }

    @GetMapping("/retention")
    public RetentionResponse getRetention(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate cohortWeekStart,
            @RequestParam(defaultValue = "1") int weeksLater) {
        return analyticsService.getRetention(cohortWeekStart, weeksLater);
    }

    @GetMapping("/dashboard")
    public DashboardResponse getDashboard(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return analyticsService.getDashboard(date);
    }
}
