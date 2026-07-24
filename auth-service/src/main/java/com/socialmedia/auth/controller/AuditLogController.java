package com.socialmedia.auth.controller;

import com.socialmedia.auth.domain.AuditLog;
import com.socialmedia.auth.security.CurrentUserProvider;
import com.socialmedia.auth.service.AuditLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth/audit-logs")
@Tag(name = "Audit Logs", description = "Security-relevant account activity (password reset, 2FA changes, device revocation, ...)")
public class AuditLogController {

    private final AuditLogService auditLogService;
    private final CurrentUserProvider currentUserProvider;

    public AuditLogController(AuditLogService auditLogService, CurrentUserProvider currentUserProvider) {
        this.auditLogService = auditLogService;
        this.currentUserProvider = currentUserProvider;
    }

    @GetMapping
    @Operation(summary = "Audit trail for the current account")
    public Page<AuditLog> list(Pageable pageable) {
        return auditLogService.listForUser(currentUserProvider.get().getId(), pageable);
    }
}
