package com.socialmedia.common.jpa;

import jakarta.persistence.EntityManagerFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Registers @EnableJpaAuditing automatically for any service that has JPA on its
 * classpath, so BaseEntity's createdAt/updatedAt are populated without every service
 * having to remember to add this itself (a real bug caught during auth-service's
 * end-to-end verification: without it, timestamps insert as null and every write fails
 * a NOT NULL constraint).
 */
@AutoConfiguration
@ConditionalOnClass(EntityManagerFactory.class)
@EnableJpaAuditing
public class JpaAuditingAutoConfiguration {
}
