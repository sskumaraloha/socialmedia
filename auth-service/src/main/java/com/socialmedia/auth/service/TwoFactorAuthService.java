package com.socialmedia.auth.service;

import com.socialmedia.auth.domain.User;
import com.socialmedia.auth.dto.response.TwoFactorSetupResponse;

public interface TwoFactorAuthService {

    /** Generates a new secret and its QR code, but does NOT enable 2FA until verify() succeeds. */
    TwoFactorSetupResponse beginSetup(User user);

    void verifyAndEnable(User user, String code);

    void disable(User user, String code);

    boolean isCodeValid(String secret, String code);
}
