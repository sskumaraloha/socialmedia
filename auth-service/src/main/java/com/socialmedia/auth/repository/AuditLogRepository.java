package com.socialmedia.auth.repository;

import com.socialmedia.auth.domain.AuditLog;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {

    Page<AuditLog> findByActorUserIdOrderByCreatedAtDesc(UUID actorUserId, Pageable pageable);
}
