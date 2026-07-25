package com.socialmedia.analytics.repository;

import com.socialmedia.analytics.domain.DailyMetrics;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface DailyMetricsRepository extends MongoRepository<DailyMetrics, LocalDate> {

    List<DailyMetrics> findByDateBetweenOrderByDateAsc(LocalDate startInclusive, LocalDate endInclusive);
}
