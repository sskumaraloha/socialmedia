package com.socialmedia.auth.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Without this, @EntityListeners(AuditingEntityListener.class) on BaseEntity is inert -
 * createdAt/updatedAt would silently stay null on every insert.
 */
@Configuration
@EnableJpaAuditing
public class JpaAuditingConfig {
}
