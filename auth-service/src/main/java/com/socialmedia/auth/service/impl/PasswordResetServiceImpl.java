package com.socialmedia.auth.service.impl;

import com.socialmedia.auth.domain.PasswordResetToken;
import com.socialmedia.auth.domain.User;
import com.socialmedia.auth.event.AuthEventPublisher;
import com.socialmedia.auth.repository.PasswordResetTokenRepository;
import com.socialmedia.auth.repository.RefreshTokenRepository;
import com.socialmedia.auth.repository.SessionRepository;
import com.socialmedia.auth.repository.UserRepository;
import com.socialmedia.auth.security.TokenHasher;
import com.socialmedia.auth.service.AuditLogService;
import com.socialmedia.auth.service.PasswordResetService;
import com.socialmedia.common.exception.BusinessException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PasswordResetServiceImpl implements PasswordResetService {

    private static final Logger log = LoggerFactory.getLogger(PasswordResetServiceImpl.class);
    private static final long TOKEN_TTL_MINUTES = 30;

    private final PasswordResetTokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final SessionRepository sessionRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthEventPublisher eventPublisher;
    private final AuditLogService auditLogService;

    public PasswordResetServiceImpl(PasswordResetTokenRepository tokenRepository, UserRepository userRepository,
            RefreshTokenRepository refreshTokenRepository, SessionRepository sessionRepository,
            PasswordEncoder passwordEncoder, AuthEventPublisher eventPublisher, AuditLogService auditLogService) {
        this.tokenRepository = tokenRepository;
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.sessionRepository = sessionRepository;
        this.passwordEncoder = passwordEncoder;
        this.eventPublisher = eventPublisher;
        this.auditLogService = auditLogService;
    }

    @Override
    @Transactional
    public void requestReset(String email) {
        userRepository.findByEmailIgnoreCase(email).ifPresentOrElse(user -> {
            String rawToken = TokenHasher.generateRawToken();
            PasswordResetToken token = new PasswordResetToken(user, TokenHasher.hash(rawToken),
                    Instant.now().plus(TOKEN_TTL_MINUTES, ChronoUnit.MINUTES));
            tokenRepository.save(token);
            eventPublisher.publishPasswordResetRequested(user.getId(), user.getEmail(), rawToken);
        }, () -> log.info("Password reset requested for unknown email - ignoring (no account enumeration)"));
    }

    @Override
    @Transactional
    public void resetPassword(String rawToken, String newPassword) {
        PasswordResetToken token = tokenRepository.findByTokenHash(TokenHasher.hash(rawToken))
                .orElseThrow(() -> new BusinessException(HttpStatus.BAD_REQUEST, "INVALID_RESET_TOKEN",
                        "Reset link is invalid or has already been used"));
        if (!token.isValid()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "EXPIRED_RESET_TOKEN",
                    "Reset link has expired - request a new one");
        }

        token.setUsed(true);
        User user = token.getUser();
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.resetFailedLoginAttempts();
        user.setAccountLocked(false);
        userRepository.save(user);

        // A password reset invalidates every existing session - if the reset was
        // triggered because credentials leaked, this logs the attacker out everywhere.
        refreshTokenRepository.revokeAllForUser(user);
        sessionRepository.revokeAllForUser(user);

        auditLogService.record(user.getId(), "PASSWORD_RESET", "USER", user.getId().toString(), null, null);
    }
}
