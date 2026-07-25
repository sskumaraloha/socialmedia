package com.socialmedia.analytics.domain;

import java.time.LocalDate;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/** One document per user - signupDate + lastActiveDate is all retention cohort analysis needs. */
@Document(collection = "user_activity")
public class UserActivity {

    @Id
    private UUID userId;

    private LocalDate signupDate;

    private LocalDate lastActiveDate;

    protected UserActivity() {
    }

    public UUID getUserId() {
        return userId;
    }

    public LocalDate getSignupDate() {
        return signupDate;
    }

    public LocalDate getLastActiveDate() {
        return lastActiveDate;
    }
}
