package com.socialmedia.auth.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.socialmedia.auth.domain.AuthProvider;
import com.socialmedia.auth.domain.Device;
import com.socialmedia.auth.domain.DeviceType;
import com.socialmedia.auth.domain.RefreshToken;
import com.socialmedia.auth.domain.Role;
import com.socialmedia.auth.domain.Session;
import com.socialmedia.auth.domain.User;
import com.socialmedia.auth.dto.request.LoginRequest;
import com.socialmedia.auth.dto.request.RegisterRequest;
import com.socialmedia.auth.dto.response.AuthTokenResponse;
import com.socialmedia.auth.dto.response.LoginResult;
import com.socialmedia.auth.dto.response.UserSummaryResponse;
import com.socialmedia.auth.event.AuthEventPublisher;
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
import com.socialmedia.auth.service.DeviceService;
import com.socialmedia.auth.service.EmailVerificationService;
import com.socialmedia.auth.service.LoginHistoryService;
import com.socialmedia.auth.service.SessionCacheService;
import com.socialmedia.auth.service.TwoFactorAuthService;
import com.socialmedia.auth.service.TwoFactorChallengeCache;
import java.lang.reflect.Field;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock private UserRepository userRepository;
    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private SessionRepository sessionRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtService jwtService;
    @Mock private SessionCacheService sessionCacheService;
    @Mock private DeviceService deviceService;
    @Mock private LoginHistoryService loginHistoryService;
    @Mock private AuditLogService auditLogService;
    @Mock private AuthEventPublisher eventPublisher;
    @Mock private TwoFactorAuthService twoFactorAuthService;
    @Mock private TwoFactorChallengeCache twoFactorChallengeCache;
    @Mock private EmailVerificationService emailVerificationService;
    @Mock private UserMapper userMapper;

    private AuthServiceImpl authService;

    @BeforeEach
    void setUp() {
        authService = new AuthServiceImpl(userRepository, refreshTokenRepository, sessionRepository, passwordEncoder,
                jwtService, sessionCacheService, deviceService, loginHistoryService, auditLogService, eventPublisher,
                twoFactorAuthService, twoFactorChallengeCache, emailVerificationService, userMapper);

        // Shared defaults used by most (not necessarily all) tests below - lenient so a
        // test that never reaches a given call path doesn't fail on "unnecessary stubbing".
        lenient().when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User user = inv.getArgument(0);
            assignIdIfAbsent(user);
            return user;
        });
        lenient().when(sessionRepository.save(any(Session.class))).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(jwtService.getAccessTokenTtlSeconds()).thenReturn(900L);
        lenient().when(jwtService.getRefreshTokenTtlSeconds()).thenReturn(2_592_000L);
        lenient().when(jwtService.generateAccessToken(any(), anyString(), any(), anyString())).thenReturn("access-token");
        lenient().when(userMapper.toSummary(any(User.class)))
                .thenReturn(new UserSummaryResponse(UUID.randomUUID(), "user@example.com", false, false,
                        AuthProvider.LOCAL, Set.of(Role.ROLE_USER)));
    }

    private User userWithId(String email, String passwordHash) throws Exception {
        User user = new User(email, passwordHash, AuthProvider.LOCAL, null);
        assignIdIfAbsent(user);
        return user;
    }

    private static void assignIdIfAbsent(User user) {
        if (user.getId() != null) {
            return;
        }
        try {
            Field idField = Class.forName("com.socialmedia.auth.domain.BaseEntity").getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(user, UUID.randomUUID());
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    @Test
    void registerCreatesUserAndReturnsTokens() {
        when(userRepository.existsByEmailIgnoreCase("new@example.com")).thenReturn(false);
        when(passwordEncoder.encode("Password123!")).thenReturn("encoded-hash");
        when(deviceService.upsert(any(User.class), eq("device-1"), any(), any()))
                .thenAnswer(inv -> new Device(inv.getArgument(0), "device-1", "My Phone", DeviceType.WEB));

        RegisterRequest request = new RegisterRequest("new@example.com", "Password123!", "device-1", "My Phone",
                DeviceType.WEB);

        AuthTokenResponse response = authService.register(request, "127.0.0.1", "JUnit");

        assertThat(response.accessToken()).isEqualTo("access-token");
        assertThat(response.refreshToken()).isNotBlank();
        verify(eventPublisher).publishUserRegistered(any(User.class));
        verify(emailVerificationService).sendVerification(any(User.class));
        verify(auditLogService).record(any(), eq("USER_REGISTERED"), eq("USER"), any(), any(), eq("127.0.0.1"));
    }

    @Test
    void registerRejectsADuplicateEmail() {
        when(userRepository.existsByEmailIgnoreCase("taken@example.com")).thenReturn(true);
        RegisterRequest request = new RegisterRequest("taken@example.com", "Password123!", "device-1", null, null);

        assertThatThrownBy(() -> authService.register(request, "127.0.0.1", "JUnit"))
                .isInstanceOf(EmailAlreadyRegisteredException.class);
        verify(userRepository, never()).save(any());
    }

    @Test
    void loginWithWrongPasswordIncrementsFailedAttemptsAndThrows() throws Exception {
        User user = userWithId("user@example.com", "stored-hash");
        when(userRepository.findByEmailIgnoreCase("user@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong-password", "stored-hash")).thenReturn(false);

        LoginRequest request = new LoginRequest("user@example.com", "wrong-password", "device-1", null, null);

        assertThatThrownBy(() -> authService.login(request, "127.0.0.1", "JUnit"))
                .isInstanceOf(InvalidCredentialsException.class);

        assertThat(user.getFailedLoginAttempts()).isEqualTo(1);
        verify(eventPublisher).publishLoginFailed("user@example.com", "127.0.0.1", "INVALID_CREDENTIALS");
    }

    @Test
    void loginWithCorrectPasswordAndNoTwoFactorReturnsTokens() throws Exception {
        User user = userWithId("user@example.com", "stored-hash");
        when(userRepository.findByEmailIgnoreCase("user@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("correct-password", "stored-hash")).thenReturn(true);
        when(deviceService.upsert(any(User.class), eq("device-1"), any(), any()))
                .thenAnswer(inv -> new Device(inv.getArgument(0), "device-1", null, DeviceType.WEB));

        LoginRequest request = new LoginRequest("user@example.com", "correct-password", "device-1", null,
                DeviceType.WEB);

        LoginResult result = authService.login(request, "127.0.0.1", "JUnit");

        assertThat(result.requiresTwoFactor()).isFalse();
        assertThat(result.tokens().accessToken()).isEqualTo("access-token");
        verify(loginHistoryService).record(user, "127.0.0.1", "JUnit", true, null);
    }

    @Test
    void loginWithTwoFactorEnabledReturnsChallengeInsteadOfTokens() throws Exception {
        User user = userWithId("user@example.com", "stored-hash");
        user.setTwoFactorEnabled(true);
        when(userRepository.findByEmailIgnoreCase("user@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("correct-password", "stored-hash")).thenReturn(true);
        when(twoFactorChallengeCache.create(any(), any())).thenReturn("challenge-token-123");

        LoginRequest request = new LoginRequest("user@example.com", "correct-password", "device-1", null, null);

        LoginResult result = authService.login(request, "127.0.0.1", "JUnit");

        assertThat(result.requiresTwoFactor()).isTrue();
        assertThat(result.tokens()).isNull();
        assertThat(result.twoFactorChallenge().challengeToken()).isEqualTo("challenge-token-123");
        verify(deviceService, never()).upsert(any(), any(), any(), any());
    }

    @Test
    void refreshRotatesTheTokenAndKeepsTheSameSession() throws Exception {
        User user = userWithId("user@example.com", "stored-hash");
        String rawRefreshToken = "raw-refresh-token";
        RefreshToken storedToken = new RefreshToken(user, TokenHasher.hash(rawRefreshToken), "device-1",
                Instant.now().plusSeconds(60));

        when(refreshTokenRepository.findByTokenHash(TokenHasher.hash(rawRefreshToken)))
                .thenReturn(Optional.of(storedToken));
        Session existingSession = new Session(user, "old-jti", "device-1", "127.0.0.1", "JUnit", Instant.now(),
                Instant.now().plusSeconds(900));
        when(sessionRepository.findByUserAndDeviceIdAndRevokedFalse(user, "device-1"))
                .thenReturn(List.of(existingSession));

        AuthTokenResponse response = authService.refresh(rawRefreshToken, "127.0.0.1", "JUnit");

        assertThat(response.accessToken()).isEqualTo("access-token");
        assertThat(existingSession.getJti()).isNotEqualTo("old-jti");
        assertThat(storedToken.isRevoked()).isTrue();
        verify(sessionCacheService).evict("old-jti");
    }

    @Test
    void refreshOfAnAlreadyRevokedTokenRevokesEverySessionForThatUser() throws Exception {
        User user = userWithId("user@example.com", "stored-hash");
        String rawRefreshToken = "stolen-refresh-token";
        RefreshToken storedToken = new RefreshToken(user, TokenHasher.hash(rawRefreshToken), "device-1",
                Instant.now().plusSeconds(60));
        storedToken.setRevoked(true);

        when(refreshTokenRepository.findByTokenHash(TokenHasher.hash(rawRefreshToken)))
                .thenReturn(Optional.of(storedToken));
        when(sessionRepository.findByUserAndRevokedFalseOrderByLastSeenAtDesc(user)).thenReturn(List.of());

        assertThatThrownBy(() -> authService.refresh(rawRefreshToken, "127.0.0.1", "JUnit"))
                .isInstanceOf(InvalidTokenException.class);

        verify(refreshTokenRepository).revokeAllForUser(user);
        verify(eventPublisher).publishSessionRevoked(user.getId(), null, null, "REFRESH_TOKEN_REUSE_DETECTED");
    }
}
