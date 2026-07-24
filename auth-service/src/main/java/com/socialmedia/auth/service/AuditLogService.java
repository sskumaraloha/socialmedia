package com.socialmedia.auth.service;

import com.socialmedia.auth.domain.AuditLog;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AuditLogService {

    void record(UUID actorUserId, String action, String targetType, String targetId, String metadataJson,
            String ipAddress);

    Page<AuditLog> listForUser(UUID actorUserId, Pageable pageable);
}
