package com.socialmedia.auth.service;

public interface PasswordResetService {

    /** Always succeeds from the caller's perspective, even for an unknown email - avoids account enumeration. */
    void requestReset(String email);

    void resetPassword(String rawToken, String newPassword);
}
