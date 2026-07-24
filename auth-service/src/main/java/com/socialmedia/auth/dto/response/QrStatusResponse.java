package com.socialmedia.auth.dto.response;

import com.socialmedia.auth.domain.QrLoginStatus;

public record QrStatusResponse(
        QrLoginStatus status,
        AuthTokenResponse tokens
) {
    public static QrStatusResponse of(QrLoginStatus status) {
        return new QrStatusResponse(status, null);
    }

    public static QrStatusResponse approved(AuthTokenResponse tokens) {
        return new QrStatusResponse(QrLoginStatus.APPROVED, tokens);
    }
}
