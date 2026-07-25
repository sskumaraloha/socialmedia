package com.socialmedia.analytics.repository;

import com.socialmedia.analytics.domain.UserActivity;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface UserActivityRepository extends MongoRepository<UserActivity, UUID> {

    long countBySignupDateBetween(LocalDate startInclusive, LocalDate endInclusive);

    long countBySignupDateBetweenAndLastActiveDateGreaterThanEqual(LocalDate startInclusive, LocalDate endInclusive,
            LocalDate activeSince);
}
