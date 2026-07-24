package com.socialmedia.auth.service;

import com.socialmedia.auth.domain.DeviceType;
import com.socialmedia.auth.domain.User;
import com.socialmedia.auth.dto.request.LoginRequest;
import com.socialmedia.auth.dto.request.RegisterRequest;
import com.socialmedia.auth.dto.response.AuthTokenResponse;
import com.socialmedia.auth.dto.response.LoginResult;
import com.socialmedia.auth.dto.response.TwoFactorChallengeResponse;

public interface AuthService {

    AuthTokenResponse register(RegisterRequest request, String ipAddress, String userAgent);

    /**
     * Returns tokens directly for accounts without 2FA. For 2FA-enabled accounts, returns
     * a challenge token instead - the client presents it, with the TOTP code, to
     * {@link #completeTwoFactorLogin}.
     */
    LoginResult login(LoginRequest request, String ipAddress, String userAgent);

    AuthTokenResponse completeTwoFactorLogin(String challengeToken, String code, String ipAddress, String userAgent);

    AuthTokenResponse refresh(String refreshTokenRaw, String ipAddress, String userAgent);

    void logout(String refreshTokenRaw, String jti);

    /** Revokes every session and refresh token for the user, including the one making this call. */
    void logoutAllDevices(User user);

    /** Shared by login/register/OAuth2/QR-login so every path issues tokens identically. */
    AuthTokenResponse issueTokensForUser(User user, String deviceId, String deviceName, DeviceType deviceType,
            String ipAddress, String userAgent);

    TwoFactorChallengeResponse issueTwoFactorChallenge(User user, String deviceId, String deviceName,
            DeviceType deviceType);
}
