package com.socialmedia.analytics.service.impl;

import com.socialmedia.analytics.domain.AuditLogEntry;
import com.socialmedia.analytics.domain.DailyMetrics;
import com.socialmedia.analytics.dto.response.AuditLogResponse;
import com.socialmedia.analytics.dto.response.DailyCount;
import com.socialmedia.analytics.dto.response.DashboardResponse;
import com.socialmedia.analytics.dto.response.DauResponse;
import com.socialmedia.analytics.dto.response.GrowthStatsResponse;
import com.socialmedia.analytics.dto.response.MauResponse;
import com.socialmedia.analytics.dto.response.MessageStatsResponse;
import com.socialmedia.analytics.dto.response.RetentionResponse;
import com.socialmedia.analytics.dto.response.RevenueStatsResponse;
import com.socialmedia.analytics.dto.response.StorageStatsResponse;
import com.socialmedia.analytics.repository.DailyMetricsRepository;
import com.socialmedia.analytics.repository.UserActivityRepository;
import com.socialmedia.analytics.service.AnalyticsService;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.ToLongFunction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

@Service
public class AnalyticsServiceImpl implements AnalyticsService {

    private static final int MAU_WINDOW_DAYS = 30;

    private final DailyMetricsRepository dailyMetricsRepository;
    private final UserActivityRepository userActivityRepository;
    private final MongoTemplate mongoTemplate;

    public AnalyticsServiceImpl(DailyMetricsRepository dailyMetricsRepository, UserActivityRepository userActivityRepository,
            MongoTemplate mongoTemplate) {
        this.dailyMetricsRepository = dailyMetricsRepository;
        this.userActivityRepository = userActivityRepository;
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public DauResponse getDau(LocalDate date) {
        int activeUsers = dailyMetricsRepository.findById(date).map(m -> m.getActiveUserIds().size()).orElse(0);
        return new DauResponse(date, activeUsers);
    }

    @Override
    public MauResponse getMau(LocalDate asOf) {
        LocalDate windowStart = asOf.minusDays(MAU_WINDOW_DAYS - 1L);
        Set<UUID> uniqueActiveUsers = new HashSet<>();
        for (DailyMetrics metrics : dailyMetricsRepository.findByDateBetweenOrderByDateAsc(windowStart, asOf)) {
            uniqueActiveUsers.addAll(metrics.getActiveUserIds());
        }
        return new MauResponse(asOf, uniqueActiveUsers.size());
    }

    @Override
    public MessageStatsResponse getMessageStats(LocalDate from, LocalDate to) {
        List<DailyMetrics> range = dailyMetricsRepository.findByDateBetweenOrderByDateAsc(from, to);
        List<DailyCount> dailyBreakdown = range.stream()
                .map(m -> new DailyCount(m.getDate(), m.getMessagesSent()))
                .toList();
        long total = sum(range, DailyMetrics::getMessagesSent);
        return new MessageStatsResponse(from, to, total, dailyBreakdown);
    }

    @Override
    public GrowthStatsResponse getGrowthStats(LocalDate from, LocalDate to) {
        List<DailyMetrics> range = dailyMetricsRepository.findByDateBetweenOrderByDateAsc(from, to);
        List<DailyCount> dailyBreakdown = range.stream()
                .map(m -> new DailyCount(m.getDate(), m.getNewSignups()))
                .toList();
        long total = sum(range, DailyMetrics::getNewSignups);
        return new GrowthStatsResponse(from, to, total, dailyBreakdown);
    }

    @Override
    public StorageStatsResponse getStorageStats(LocalDate from, LocalDate to) {
        List<DailyMetrics> range = dailyMetricsRepository.findByDateBetweenOrderByDateAsc(from, to);
        List<DailyCount> dailyBreakdown = range.stream()
                .map(m -> new DailyCount(m.getDate(), m.getStorageBytesUploaded()))
                .toList();
        long total = sum(range, DailyMetrics::getStorageBytesUploaded);
        return new StorageStatsResponse(from, to, total, dailyBreakdown);
    }

    @Override
    public RevenueStatsResponse getRevenueStats(LocalDate from, LocalDate to) {
        List<DailyMetrics> range = dailyMetricsRepository.findByDateBetweenOrderByDateAsc(from, to);
        List<DailyCount> dailyBreakdown = range.stream()
                .map(m -> new DailyCount(m.getDate(), m.getRevenueCents()))
                .toList();
        long total = sum(range, DailyMetrics::getRevenueCents);
        return new RevenueStatsResponse(from, to, total, dailyBreakdown);
    }

    @Override
    public RetentionResponse getRetention(LocalDate cohortWeekStart, int weeksLater) {
        LocalDate cohortEndExclusive = cohortWeekStart.plusWeeks(1);
        long cohortSize = userActivityRepository.countBySignupDateBetween(cohortWeekStart, cohortEndExclusive.minusDays(1));
        LocalDate activeSince = cohortWeekStart.plusWeeks(weeksLater);
        long retainedCount = userActivityRepository.countBySignupDateBetweenAndLastActiveDateGreaterThanEqual(
                cohortWeekStart, cohortEndExclusive.minusDays(1), activeSince);
        double retentionRate = cohortSize == 0 ? 0.0 : (double) retainedCount / cohortSize;
        return new RetentionResponse(cohortWeekStart, weeksLater, cohortSize, retainedCount, retentionRate);
    }

    @Override
    public DashboardResponse getDashboard(LocalDate date) {
        Optional<DailyMetrics> today = dailyMetricsRepository.findById(date);
        int dau = today.map(m -> m.getActiveUserIds().size()).orElse(0);
        int mau = getMau(date).activeUsers();
        long newSignups = today.map(DailyMetrics::getNewSignups).orElse(0L);
        long messages = today.map(DailyMetrics::getMessagesSent).orElse(0L);
        long storageBytes = today.map(DailyMetrics::getStorageBytesUploaded).orElse(0L);
        long revenueCents = today.map(DailyMetrics::getRevenueCents).orElse(0L);
        return new DashboardResponse(date, dau, mau, newSignups, messages, storageBytes, revenueCents);
    }

    @Override
    public Page<AuditLogResponse> getAuditLogs(UUID actorUserId, String action, String targetType, Pageable pageable) {
        Criteria criteria = new Criteria();
        List<Criteria> filters = new java.util.ArrayList<>();
        if (actorUserId != null) {
            filters.add(Criteria.where("actorUserId").is(actorUserId));
        }
        if (action != null && !action.isBlank()) {
            filters.add(Criteria.where("action").is(action));
        }
        if (targetType != null && !targetType.isBlank()) {
            filters.add(Criteria.where("targetType").is(targetType));
        }
        if (!filters.isEmpty()) {
            criteria.andOperator(filters.toArray(new Criteria[0]));
        }

        Query query = Query.query(criteria).with(pageable);
        List<AuditLogResponse> content = mongoTemplate.find(query, AuditLogEntry.class).stream()
                .map(e -> new AuditLogResponse(e.getActorUserId(), e.getAction(), e.getTargetType(), e.getTargetId(),
                        e.getMetadata(), e.getOccurredAt()))
                .toList();
        return PageableExecutionUtils.getPage(content, pageable,
                () -> mongoTemplate.count(Query.query(criteria), AuditLogEntry.class));
    }

    private long sum(List<DailyMetrics> metrics, ToLongFunction<DailyMetrics> extractor) {
        return metrics.stream().mapToLong(extractor).sum();
    }
}
