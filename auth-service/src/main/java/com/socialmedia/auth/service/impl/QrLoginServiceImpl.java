package com.socialmedia.auth.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.socialmedia.auth.domain.QrLoginSession;
import com.socialmedia.auth.domain.QrLoginStatus;
import com.socialmedia.auth.domain.User;
import com.socialmedia.auth.dto.request.QrInitRequest;
import com.socialmedia.auth.dto.response.AuthTokenResponse;
import com.socialmedia.auth.dto.response.QrInitResponse;
import com.socialmedia.auth.dto.response.QrStatusResponse;
import com.socialmedia.auth.repository.UserRepository;
import com.socialmedia.auth.security.TokenHasher;
import com.socialmedia.auth.service.AuthService;
import com.socialmedia.auth.service.QrLoginService;
import com.socialmedia.common.exception.BusinessException;
import com.socialmedia.common.exception.ResourceNotFoundException;
import java.io.ByteArrayOutputStream;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

/**
 * QR-login state lives entirely in Redis as plain JSON (see QrLoginSession) - a
 * ~90s-lived, high-churn flow has no business being in Postgres, and JSON strings via
 * StringRedisTemplate avoid Jackson's polymorphic-typing pitfalls entirely since this
 * always reads back the one concrete type it wrote. init() is called by the
 * unauthenticated requesting client (e.g. a browser); approve()/deny() by the
 * already-authenticated device that scanned the code; poll() by the requesting client,
 * which is where tokens actually get minted once approved.
 */
@Service
public class QrLoginServiceImpl implements QrLoginService {

    private static final String KEY_PREFIX = "auth:qrlogin:";
    private static final Duration QR_TTL = Duration.ofSeconds(90);
    private static final int QR_IMAGE_SIZE = 320;

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final UserRepository userRepository;
    private final AuthService authService;

    public QrLoginServiceImpl(StringRedisTemplate redisTemplate, ObjectMapper objectMapper,
            UserRepository userRepository, AuthService authService) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.userRepository = userRepository;
        this.authService = authService;
    }

    @Override
    public QrInitResponse init(QrInitRequest request) {
        String qrToken = TokenHasher.generateRawToken();
        Instant now = Instant.now();
        QrLoginSession session = new QrLoginSession(qrToken, QrLoginStatus.PENDING, request.deviceId(), now,
                now.plus(QR_TTL));
        save(session);

        String qrImageBase64 = renderQrCodePng("socialmedia://qr-login?token=" + qrToken);
        return new QrInitResponse(qrToken, qrImageBase64, QR_TTL.getSeconds());
    }

    @Override
    public void approve(UUID approvingUserId, String approvingDeviceId, String qrToken) {
        QrLoginSession session = require(qrToken);
        assertPending(session);
        session.setStatus(QrLoginStatus.APPROVED);
        session.setUserId(approvingUserId);
        session.setApprovedByDeviceId(approvingDeviceId);
        save(session);
    }

    @Override
    public void deny(String qrToken) {
        QrLoginSession session = require(qrToken);
        assertPending(session);
        session.setStatus(QrLoginStatus.DENIED);
        save(session);
    }

    @Override
    public QrStatusResponse poll(String qrToken, String ipAddress, String userAgent) {
        QrLoginSession session = require(qrToken);

        if (session.isExpired()) {
            return QrStatusResponse.of(QrLoginStatus.EXPIRED);
        }
        if (session.getStatus() != QrLoginStatus.APPROVED) {
            return QrStatusResponse.of(session.getStatus());
        }

        // Consume immediately so a repeated poll after this point can never mint a second token pair.
        redisTemplate.delete(key(qrToken));

        User user = userRepository.findById(session.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User", session.getUserId()));
        AuthTokenResponse tokens = authService.issueTokensForUser(user, session.getRequestingDeviceId(), "QR login",
                null, ipAddress, userAgent);
        return QrStatusResponse.approved(tokens);
    }

    private void assertPending(QrLoginSession session) {
        if (session.isExpired()) {
            throw new BusinessException(HttpStatus.GONE, "QR_LOGIN_EXPIRED", "QR login code has expired");
        }
        if (session.getStatus() != QrLoginStatus.PENDING) {
            throw new BusinessException(HttpStatus.CONFLICT, "QR_LOGIN_ALREADY_RESOLVED",
                    "This QR login code has already been " + session.getStatus().name().toLowerCase());
        }
    }

    private QrLoginSession require(String qrToken) {
        String json = redisTemplate.opsForValue().get(key(qrToken));
        if (json == null) {
            throw new ResourceNotFoundException("QR login session", qrToken);
        }
        try {
            return objectMapper.readValue(json, QrLoginSession.class);
        } catch (Exception e) {
            throw new ResourceNotFoundException("QR login session", qrToken);
        }
    }

    private void save(QrLoginSession session) {
        Duration ttl = Duration.between(Instant.now(), session.getExpiresAt());
        if (ttl.isNegative() || ttl.isZero()) {
            ttl = Duration.ofSeconds(1);
        }
        try {
            String json = objectMapper.writeValueAsString(session);
            redisTemplate.opsForValue().set(key(session.getQrToken()), json, ttl);
        } catch (Exception e) {
            throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "QR_LOGIN_SAVE_FAILED",
                    "Failed to persist QR login state");
        }
    }

    private String renderQrCodePng(String content) {
        try {
            BitMatrix matrix = new QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, QR_IMAGE_SIZE, QR_IMAGE_SIZE);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(matrix, "PNG", out);
            return "data:image/png;base64," + Base64.getEncoder().encodeToString(out.toByteArray());
        } catch (Exception e) {
            throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "QR_GENERATION_FAILED",
                    "Failed to generate QR login code");
        }
    }

    private String key(String qrToken) {
        return KEY_PREFIX + qrToken;
    }
}
