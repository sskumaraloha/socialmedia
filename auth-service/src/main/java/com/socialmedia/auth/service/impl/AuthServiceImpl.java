package com.socialmedia.auth.service.impl;

import com.socialmedia.auth.domain.AuthProvider;
import com.socialmedia.auth.domain.Device;
import com.socialmedia.auth.domain.DeviceType;
import com.socialmedia.auth.domain.RefreshToken;
import com.socialmedia.auth.domain.Session;
import com.socialmedia.auth.domain.User;
import com.socialmedia.auth.dto.request.LoginRequest;
import com.socialmedia.auth.dto.request.RegisterRequest;
import com.socialmedia.auth.dto.response.AuthTokenResponse;
import com.socialmedia.auth.dto.response.LoginResult;
import com.socialmedia.auth.dto.response.TwoFactorChallengeResponse;
import com.socialmedia.auth.event.AuthEventPublisher;
import com.socialmedia.auth.exception.AccountLockedException;
import com.socialmedia.auth.exception.EmailAlreadyRegisteredException;
import com.socialmedia.auth.exception.InvalidCredentialsException;
import com.socialmedia.auth.exception.InvalidTokenException;
import com.socialmedia.auth.mapper.UserMapper;
import com.socialmedia.auth.repository.RefreshTokenRepository;
import com.socialmedia.auth.repository.SessionRepository;
import com.socialmedia.auth.repository.UserRepository;
import com.socialmedia.auth.security.JwtService;
import com.socialmedia.auth.security.TokenHasher;
import com.socialmedia.auth.service.AuditLogService;
import com.socialmedia.auth.service.AuthService;
import com.socialmedia.auth.service.DeviceService;
import com.socialmedia.auth.service.EmailVerificationService;
import com.socialmedia.auth.service.LoginHistoryService;
import com.socialmedia.auth.service.SessionCacheService;
import com.socialmedia.auth.service.TwoFactorAuthService;
import com.socialmedia.auth.service.TwoFactorChallengeCache;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthServiceImpl implements AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthServiceImpl.class);

    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final Duration ACCOUNT_LOCK_DURATION = Duration.ofMinutes(15);
    private static final Duration TWO_FACTOR_CHALLENGE_TTL = Duration.ofMinutes(5);

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final SessionRepository sessionRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final SessionCacheService sessionCacheService;
    private final DeviceService deviceService;
    private final LoginHistoryService loginHistoryService;
    private final AuditLogService auditLogService;
    private final AuthEventPublisher eventPublisher;
    private final TwoFactorAuthService twoFactorAuthService;
    private final TwoFactorChallengeCache twoFactorChallengeCache;
    private final EmailVerificationService emailVerificationService;
    private final UserMapper userMapper;

    public AuthServiceImpl(UserRepository userRepository, RefreshTokenRepository refreshTokenRepository,
            SessionRepository sessionRepository, PasswordEncoder passwordEncoder, JwtService jwtService,
            SessionCacheService sessionCacheService, DeviceService deviceService,
            LoginHistoryService loginHistoryService, AuditLogService auditLogService,
            AuthEventPublisher eventPublisher, TwoFactorAuthService twoFactorAuthService,
            TwoFactorChallengeCache twoFactorChallengeCache, EmailVerificationService emailVerificationService,
            UserMapper userMapper) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.sessionRepository = sessionRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.sessionCacheService = sessionCacheService;
        this.deviceService = deviceService;
        this.loginHistoryService = loginHistoryService;
        this.auditLogService = auditLogService;
        this.eventPublisher = eventPublisher;
        this.twoFactorAuthService = twoFactorAuthService;
        this.twoFactorChallengeCache = twoFactorChallengeCache;
        this.emailVerificationService = emailVerificationService;
        this.userMapper = userMapper;
    }

    @Override
    @Transactional
    public AuthTokenResponse register(RegisterRequest request, String ipAddress, String userAgent) {
        if (userRepository.existsByEmailIgnoreCase(request.email())) {
            throw new EmailAlreadyRegisteredException(request.email());
        }

        User user = new User(request.email(), passwordEncoder.encode(request.password()), AuthProvider.LOCAL, null);
        user = userRepository.save(user);

        eventPublisher.publishUserRegistered(user);
        emailVerificationService.sendVerification(user);
        auditLogService.record(user.getId(), "USER_REGISTERED", "USER", user.getId().toString(), null, ipAddress);

        return issueTokensForUser(user, request.deviceId(), request.deviceName(), request.deviceType(),
                ipAddress, userAgent);
    }

    @Override
    @Transactional
    public LoginResult login(LoginRequest request, String ipAddress, String userAgent) {
        Optional<User> maybeUser = userRepository.findByEmailIgnoreCase(request.email());

        User user = maybeUser.orElse(null);
        if (user == null || user.getPasswordHash() == null
                || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            if (user != null) {
                registerFailedAttempt(user);
            }
            loginHistoryService.record(user, ipAddress, userAgent, false, "INVALID_CREDENTIALS");
            eventPublisher.publishLoginFailed(request.email(), ipAddress, "INVALID_CREDENTIALS");
            throw new InvalidCredentialsException();
        }

        if (user.isAccountLocked()) {
            if (user.getLockedUntil() != null && user.getLockedUntil().isAfter(Instant.now())) {
                loginHistoryService.record(user, ipAddress, userAgent, false, "ACCOUNT_LOCKED");
                throw new AccountLockedException();
            }
            // Lock window elapsed - clear it and let this attempt proceed normally.
            user.setAccountLocked(false);
            user.resetFailedLoginAttempts();
        }

        user.resetFailedLoginAttempts();

        if (user.isTwoFactorEnabled()) {
            TwoFactorChallengeResponse challenge = issueTwoFactorChallenge(user, request.deviceId(),
                    request.deviceName(), request.deviceType());
            return LoginResult.ofChallenge(challenge);
        }

        loginHistoryService.record(user, ipAddress, userAgent, true, null);
        AuthTokenResponse tokens = issueTokensForUser(user, request.deviceId(), request.deviceName(),
                request.deviceType(), ipAddress, userAgent);
        return LoginResult.ofTokens(tokens);
    }

    private void registerFailedAttempt(User user) {
        user.incrementFailedLoginAttempts();
        if (user.getFailedLoginAttempts() >= MAX_FAILED_ATTEMPTS) {
            user.setAccountLocked(true);
            user.setLockedUntil(Instant.now().plus(ACCOUNT_LOCK_DURATION));
        }
        userRepository.save(user);
    }

    @Override
    @Transactional(readOnly = true)
    public TwoFactorChallengeResponse issueTwoFactorChallenge(User user, String deviceId, String deviceName,
            DeviceType deviceType) {
        String challengeToken = twoFactorChallengeCache.create(
                new TwoFactorChallengeCache.Challenge(user.getId(), deviceId, deviceName, deviceType, null, null),
                TWO_FACTOR_CHALLENGE_TTL);
        return new TwoFactorChallengeResponse(challengeToken, "Enter your authenticator code to finish signing in",
                TWO_FACTOR_CHALLENGE_TTL.getSeconds());
    }

    @Override
    @Transactional
    public AuthTokenResponse completeTwoFactorLogin(String challengeToken, String code, String ipAddress,
            String userAgent) {
        TwoFactorChallengeCache.Challenge challenge = twoFactorChallengeCache.get(challengeToken)
                .orElseThrow(() -> new InvalidTokenException("Two-factor challenge has expired - please log in again"));

        User user = userRepository.findById(challenge.userId())
                .orElseThrow(() -> new InvalidTokenException("Account no longer exists"));

        if (!twoFactorAuthService.isCodeValid(user.getTwoFactorSecret(), code)) {
            loginHistoryService.record(user, ipAddress, userAgent, false, "INVALID_2FA_CODE");
            throw new InvalidCredentialsException();
        }

        twoFactorChallengeCache.evict(challengeToken);
        loginHistoryService.record(user, ipAddress, userAgent, true, null);
        return issueTokensForUser(user, challenge.deviceId(), challenge.deviceName(), challenge.deviceType(),
                ipAddress, userAgent);
    }

    @Override
    @Transactional
    public AuthTokenResponse issueTokensForUser(User user, String deviceId, String deviceName, DeviceType deviceType,
            String ipAddress, String userAgent) {
        Device device = deviceService.upsert(user, deviceId, deviceName, deviceType);

        String jti = UUID.randomUUID().toString();
        Instant now = Instant.now();
        Instant accessExpiresAt = now.plusSeconds(jwtService.getAccessTokenTtlSeconds());

        Session session = new Session(user, jti, device.getDeviceId(), ipAddress, userAgent, now, accessExpiresAt);
        sessionRepository.save(session);
        sessionCacheService.put(jti, user.getId(), device.getDeviceId(), Duration.ofSeconds(jwtService.getAccessTokenTtlSeconds()));

        String accessToken = jwtService.generateAccessToken(user.getId(), jti, user.getRoles(), device.getDeviceId());
        String refreshTokenRaw = TokenHasher.generateRawToken();
        refreshTokenRepository.save(new RefreshToken(user, TokenHasher.hash(refreshTokenRaw), device.getDeviceId(),
                now.plusSeconds(jwtService.getRefreshTokenTtlSeconds())));

        eventPublisher.publishUserLoggedIn(user.getId(), device.getDeviceId(), ipAddress);

        return AuthTokenResponse.bearer(accessToken, refreshTokenRaw, jwtService.getAccessTokenTtlSeconds(),
                userMapper.toSummary(user));
    }

    @Override
    @Transactional
    public AuthTokenResponse refresh(String refreshTokenRaw, String ipAddress, String userAgent) {
        String hash = TokenHasher.hash(refreshTokenRaw);
        RefreshToken token = refreshTokenRepository.findByTokenHash(hash)
                .orElseThrow(() -> new InvalidTokenException("Refresh token is invalid"));

        if (token.isRevoked()) {
            // Reuse of a rotated-away token is a strong signal of theft - kill every
            // session for this user rather than trusting any single device.
            log.warn("Refresh token reuse detected for user {} - revoking all sessions", token.getUser().getId());
            refreshTokenRepository.revokeAllForUser(token.getUser());
            revokeAllSessionEntities(token.getUser());
            eventPublisher.publishSessionRevoked(token.getUser().getId(), null, null, "REFRESH_TOKEN_REUSE_DETECTED");
            throw new InvalidTokenException("Refresh token has already been used - all sessions revoked");
        }
        if (token.isExpired()) {
            throw new InvalidTokenException("Refresh token has expired");
        }

        User user = token.getUser();
        String newJti = UUID.randomUUID().toString();
        Instant now = Instant.now();
        Instant accessExpiresAt = now.plusSeconds(jwtService.getAccessTokenTtlSeconds());

        List<Session> deviceSessions = sessionRepository.findByUserAndDeviceIdAndRevokedFalse(user, token.getDeviceId());
        Session session = deviceSessions.isEmpty()
                ? sessionRepository.save(new Session(user, newJti, token.getDeviceId(), ipAddress, userAgent, now, accessExpiresAt))
                : deviceSessions.get(0);
        if (!deviceSessions.isEmpty()) {
            sessionCacheService.evict(session.getJti());
            session.rotate(newJti, now, accessExpiresAt);
        }
        sessionCacheService.put(newJti, user.getId(), token.getDeviceId(),
                Duration.ofSeconds(jwtService.getAccessTokenTtlSeconds()));

        String newRefreshTokenRaw = TokenHasher.generateRawToken();
        RefreshToken newToken = new RefreshToken(user, TokenHasher.hash(newRefreshTokenRaw), token.getDeviceId(),
                now.plusSeconds(jwtService.getRefreshTokenTtlSeconds()));
        refreshTokenRepository.save(newToken);

        token.setRevoked(true);
        token.setReplacedByTokenHash(newToken.getTokenHash());

        String accessToken = jwtService.generateAccessToken(user.getId(), newJti, user.getRoles(), token.getDeviceId());

        return AuthTokenResponse.bearer(accessToken, newRefreshTokenRaw, jwtService.getAccessTokenTtlSeconds(),
                userMapper.toSummary(user));
    }

    @Override
    @Transactional
    public void logout(String refreshTokenRaw, String jti) {
        refreshTokenRepository.findByTokenHash(TokenHasher.hash(refreshTokenRaw))
                .ifPresent(token -> token.setRevoked(true));
        if (jti != null) {
            sessionRepository.findByJti(jti).ifPresent(session -> session.setRevoked(true));
            sessionCacheService.evict(jti);
        }
    }

    @Override
    @Transactional
    public void logoutAllDevices(User user) {
        refreshTokenRepository.revokeAllForUser(user);
        revokeAllSessionEntities(user);
        eventPublisher.publishSessionRevoked(user.getId(), null, null, "LOGOUT_ALL_DEVICES");
    }

    private void revokeAllSessionEntities(User user) {
        for (Session session : sessionRepository.findByUserAndRevokedFalseOrderByLastSeenAtDesc(user)) {
            session.setRevoked(true);
            sessionCacheService.evict(session.getJti());
        }
    }
}
