package com.socialmedia.analytics.repository;

import com.socialmedia.analytics.domain.AuditLogEntry;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface AuditLogEntryRepository extends MongoRepository<AuditLogEntry, String> {
}
