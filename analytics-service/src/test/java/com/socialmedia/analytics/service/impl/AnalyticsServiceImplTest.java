package com.socialmedia.analytics.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import com.socialmedia.analytics.domain.AuditLogEntry;
import com.socialmedia.analytics.domain.DailyMetrics;
import com.socialmedia.analytics.dto.response.AuditLogResponse;
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
import java.lang.reflect.Field;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;

@ExtendWith(MockitoExtension.class)
class AnalyticsServiceImplTest {

    @Mock private DailyMetricsRepository dailyMetricsRepository;
    @Mock private UserActivityRepository userActivityRepository;
    @Mock private MongoTemplate mongoTemplate;

    private AnalyticsServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new AnalyticsServiceImpl(dailyMetricsRepository, userActivityRepository, mongoTemplate);
    }

    private DailyMetrics metrics(LocalDate date, long newSignups, Set<UUID> activeUserIds, long messagesSent,
            long storageBytesUploaded, long revenueCents) {
        DailyMetrics m = new DailyMetrics(date);
        set(m, "newSignups", newSignups);
        set(m, "activeUserIds", activeUserIds);
        set(m, "messagesSent", messagesSent);
        set(m, "storageBytesUploaded", storageBytesUploaded);
        set(m, "revenueCents", revenueCents);
        return m;
    }

    private void set(DailyMetrics m, String field, Object value) {
        try {
            Field f = DailyMetrics.class.getDeclaredField(field);
            f.setAccessible(true);
            f.set(m, value);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void getDau_returnsActiveUserCountForDate() {
        LocalDate date = LocalDate.of(2026, 7, 25);
        Set<UUID> active = Set.of(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());
        when(dailyMetricsRepository.findById(date)).thenReturn(Optional.of(metrics(date, 0, active, 0, 0, 0)));

        DauResponse result = service.getDau(date);

        assertThat(result.date()).isEqualTo(date);
        assertThat(result.activeUsers()).isEqualTo(3);
    }

    @Test
    void getDau_returnsZeroWhenNoDocumentExists() {
        LocalDate date = LocalDate.of(2026, 7, 25);
        when(dailyMetricsRepository.findById(date)).thenReturn(Optional.empty());

        DauResponse result = service.getDau(date);

        assertThat(result.activeUsers()).isZero();
    }

    @Test
    void getMau_unionsActiveUserIdsAcrossTrailing30DayWindow() {
        LocalDate asOf = LocalDate.of(2026, 7, 25);
        UUID shared = UUID.randomUUID();
        UUID onlyDay1 = UUID.randomUUID();
        UUID onlyDay2 = UUID.randomUUID();
        DailyMetrics day1 = metrics(asOf.minusDays(10), 0, Set.of(shared, onlyDay1), 0, 0, 0);
        DailyMetrics day2 = metrics(asOf, 0, Set.of(shared, onlyDay2), 0, 0, 0);
        when(dailyMetricsRepository.findByDateBetweenOrderByDateAsc(asOf.minusDays(29), asOf))
                .thenReturn(List.of(day1, day2));

        MauResponse result = service.getMau(asOf);

        assertThat(result.activeUsers()).isEqualTo(3);
    }

    @Test
    void getMessageStats_sumsAndBreaksDownDailyMessageCounts() {
        LocalDate from = LocalDate.of(2026, 7, 20);
        LocalDate to = LocalDate.of(2026, 7, 21);
        DailyMetrics d1 = metrics(from, 0, new HashSet<>(), 100, 0, 0);
        DailyMetrics d2 = metrics(to, 0, new HashSet<>(), 50, 0, 0);
        when(dailyMetricsRepository.findByDateBetweenOrderByDateAsc(from, to)).thenReturn(List.of(d1, d2));

        MessageStatsResponse result = service.getMessageStats(from, to);

        assertThat(result.totalMessages()).isEqualTo(150);
        assertThat(result.dailyBreakdown()).hasSize(2);
        assertThat(result.dailyBreakdown().get(0).date()).isEqualTo(from);
        assertThat(result.dailyBreakdown().get(0).count()).isEqualTo(100);
    }

    @Test
    void getGrowthStats_sumsNewSignups() {
        LocalDate from = LocalDate.of(2026, 7, 20);
        LocalDate to = LocalDate.of(2026, 7, 21);
        when(dailyMetricsRepository.findByDateBetweenOrderByDateAsc(from, to)).thenReturn(
                List.of(metrics(from, 5, new HashSet<>(), 0, 0, 0), metrics(to, 7, new HashSet<>(), 0, 0, 0)));

        GrowthStatsResponse result = service.getGrowthStats(from, to);

        assertThat(result.totalNewUsers()).isEqualTo(12);
    }

    @Test
    void getStorageStats_sumsStorageBytes() {
        LocalDate from = LocalDate.of(2026, 7, 20);
        LocalDate to = LocalDate.of(2026, 7, 21);
        when(dailyMetricsRepository.findByDateBetweenOrderByDateAsc(from, to)).thenReturn(
                List.of(metrics(from, 0, new HashSet<>(), 0, 1000, 0), metrics(to, 0, new HashSet<>(), 0, 2000, 0)));

        StorageStatsResponse result = service.getStorageStats(from, to);

        assertThat(result.totalBytesUploaded()).isEqualTo(3000);
    }

    @Test
    void getRevenueStats_sumsRevenueCents() {
        LocalDate from = LocalDate.of(2026, 7, 20);
        LocalDate to = LocalDate.of(2026, 7, 21);
        when(dailyMetricsRepository.findByDateBetweenOrderByDateAsc(from, to)).thenReturn(
                List.of(metrics(from, 0, new HashSet<>(), 0, 0, 500), metrics(to, 0, new HashSet<>(), 0, 0, 750)));

        RevenueStatsResponse result = service.getRevenueStats(from, to);

        assertThat(result.totalRevenueCents()).isEqualTo(1250);
    }

    @Test
    void getRetention_computesRetentionRateForCohortWeek() {
        LocalDate cohortWeekStart = LocalDate.of(2026, 7, 6);
        when(userActivityRepository.countBySignupDateBetween(cohortWeekStart, cohortWeekStart.plusDays(6)))
                .thenReturn(100L);
        when(userActivityRepository.countBySignupDateBetweenAndLastActiveDateGreaterThanEqual(
                cohortWeekStart, cohortWeekStart.plusDays(6), cohortWeekStart.plusWeeks(1)))
                .thenReturn(40L);

        RetentionResponse result = service.getRetention(cohortWeekStart, 1);

        assertThat(result.cohortSize()).isEqualTo(100);
        assertThat(result.retainedCount()).isEqualTo(40);
        assertThat(result.retentionRate()).isEqualTo(0.4);
    }

    @Test
    void getRetention_returnsZeroRateWhenCohortIsEmpty() {
        LocalDate cohortWeekStart = LocalDate.of(2026, 7, 6);
        when(userActivityRepository.countBySignupDateBetween(any(), any())).thenReturn(0L);

        RetentionResponse result = service.getRetention(cohortWeekStart, 2);

        assertThat(result.retentionRate()).isZero();
    }

    @Test
    void getDashboard_combinesTodayMetricsWithMau() {
        LocalDate date = LocalDate.of(2026, 7, 25);
        Set<UUID> active = Set.of(UUID.randomUUID(), UUID.randomUUID());
        DailyMetrics today = metrics(date, 3, active, 42, 8192, 999);
        when(dailyMetricsRepository.findById(date)).thenReturn(Optional.of(today));
        when(dailyMetricsRepository.findByDateBetweenOrderByDateAsc(date.minusDays(29), date))
                .thenReturn(List.of(today));

        DashboardResponse result = service.getDashboard(date);

        assertThat(result.dau()).isEqualTo(2);
        assertThat(result.mau()).isEqualTo(2);
        assertThat(result.newSignupsToday()).isEqualTo(3);
        assertThat(result.messagesToday()).isEqualTo(42);
        assertThat(result.storageBytesToday()).isEqualTo(8192);
        assertThat(result.revenueCentsToday()).isEqualTo(999);
    }

    @Test
    void getDashboard_defaultsToZeroWhenNoDataForToday() {
        LocalDate date = LocalDate.of(2026, 7, 25);
        when(dailyMetricsRepository.findById(date)).thenReturn(Optional.empty());
        when(dailyMetricsRepository.findByDateBetweenOrderByDateAsc(date.minusDays(29), date))
                .thenReturn(List.of());

        DashboardResponse result = service.getDashboard(date);

        assertThat(result.dau()).isZero();
        assertThat(result.mau()).isZero();
        assertThat(result.newSignupsToday()).isZero();
    }

    @Test
    void getAuditLogs_mapsEntriesAndReturnsTotalCount() {
        UUID actorId = UUID.randomUUID();
        Instant occurredAt = Instant.parse("2026-07-25T10:00:00Z");
        AuditLogEntry entry = new AuditLogEntry(actorId, "CHAT_DELETED", "CHAT", "chat-123",
                Map.of("chatType", "GROUP"), occurredAt);
        Pageable pageable = PageRequest.of(0, 20);
        when(mongoTemplate.find(any(Query.class), eq(AuditLogEntry.class))).thenReturn(List.of(entry));
        lenient().when(mongoTemplate.count(any(Query.class), eq(AuditLogEntry.class))).thenReturn(1L);

        Page<AuditLogResponse> result = service.getAuditLogs(actorId, "CHAT_DELETED", "CHAT", pageable);

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent()).hasSize(1);
        AuditLogResponse response = result.getContent().get(0);
        assertThat(response.actorUserId()).isEqualTo(actorId);
        assertThat(response.action()).isEqualTo("CHAT_DELETED");
        assertThat(response.targetId()).isEqualTo("chat-123");
        assertThat(response.metadata()).containsEntry("chatType", "GROUP");
    }

    @Test
    void getAuditLogs_returnsEmptyPageWhenNothingMatches() {
        Pageable pageable = PageRequest.of(0, 20);
        when(mongoTemplate.find(any(Query.class), eq(AuditLogEntry.class))).thenReturn(List.of());
        lenient().when(mongoTemplate.count(any(Query.class), eq(AuditLogEntry.class))).thenReturn(0L);

        Page<AuditLogResponse> result = service.getAuditLogs(null, null, null, pageable);

        assertThat(result.getTotalElements()).isZero();
        assertThat(result.getContent()).isEmpty();
    }
}
