package com.socialmedia.analytics.service.impl;

import static org.springframework.data.mongodb.core.query.Criteria.where;
import static org.springframework.data.mongodb.core.query.Query.query;

import com.socialmedia.analytics.domain.DailyMetrics;
import com.socialmedia.analytics.domain.UserActivity;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;

/**
 * Every write here is an atomic MongoDB upsert ($inc / $addToSet / $max), never a
 * read-modify-write of a loaded Java object - multiple Kafka consumer threads increment
 * the same day's document concurrently, and a read-modify-write would lose updates under
 * that concurrency.
 */
@Component
public class MetricsWriter {

    private final MongoTemplate mongoTemplate;

    public MetricsWriter(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    public void recordSignup(LocalDate date) {
        mongoTemplate.upsert(query(where("_id").is(date)), new Update().inc("newSignups", 1), DailyMetrics.class);
    }

    public void recordActiveUser(LocalDate date, UUID userId) {
        mongoTemplate.upsert(query(where("_id").is(date)), new Update().addToSet("activeUserIds", userId), DailyMetrics.class);
    }

    public void recordMessage(LocalDate date) {
        mongoTemplate.upsert(query(where("_id").is(date)), new Update().inc("messagesSent", 1), DailyMetrics.class);
    }

    public void recordStorage(LocalDate date, long bytes) {
        mongoTemplate.upsert(query(where("_id").is(date)), new Update().inc("storageBytesUploaded", bytes), DailyMetrics.class);
    }

    public void recordRevenue(LocalDate date, long cents) {
        mongoTemplate.upsert(query(where("_id").is(date)), new Update().inc("revenueCents", cents), DailyMetrics.class);
    }

    public void recordSignupActivity(UUID userId, LocalDate date) {
        mongoTemplate.upsert(query(where("_id").is(userId)),
                new Update().setOnInsert("signupDate", date).setOnInsert("lastActiveDate", date), UserActivity.class);
    }

    public void recordLoginActivity(UUID userId, LocalDate date) {
        mongoTemplate.upsert(query(where("_id").is(userId)),
                new Update().max("lastActiveDate", date).setOnInsert("signupDate", date), UserActivity.class);
    }
}
