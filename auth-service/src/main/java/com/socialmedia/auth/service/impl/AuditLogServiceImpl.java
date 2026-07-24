package com.socialmedia.auth.service.impl;

import com.socialmedia.auth.domain.AuditLog;
import com.socialmedia.auth.repository.AuditLogRepository;
import com.socialmedia.auth.service.AuditLogService;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditLogServiceImpl implements AuditLogService {

    private final AuditLogRepository auditLogRepository;

    public AuditLogServiceImpl(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Override
    @Transactional
    public void record(UUID actorUserId, String action, String targetType, String targetId, String metadataJson,
            String ipAddress) {
        auditLogRepository.save(new AuditLog(actorUserId, action, targetType, targetId, metadataJson, ipAddress));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AuditLog> listForUser(UUID actorUserId, Pageable pageable) {
        return auditLogRepository.findByActorUserIdOrderByCreatedAtDesc(actorUserId, pageable);
    }
}
