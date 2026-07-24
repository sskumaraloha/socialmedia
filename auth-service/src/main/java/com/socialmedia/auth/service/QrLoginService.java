package com.socialmedia.auth.service;

import com.socialmedia.auth.dto.request.QrInitRequest;
import com.socialmedia.auth.dto.response.QrInitResponse;
import com.socialmedia.auth.dto.response.QrStatusResponse;
import java.util.UUID;

public interface QrLoginService {

    QrInitResponse init(QrInitRequest request);

    /** Called by the already-authenticated mobile app after scanning the code. */
    void approve(UUID approvingUserId, String approvingDeviceId, String qrToken);

    void deny(String qrToken);

    /**
     * Polled by the requesting (e.g. web) client. Once APPROVED, this call mints tokens
     * and consumes the QR session so a repeated poll can't re-issue them.
     */
    QrStatusResponse poll(String qrToken, String ipAddress, String userAgent);
}
