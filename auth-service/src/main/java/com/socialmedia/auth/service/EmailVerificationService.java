package com.socialmedia.auth.service;

import com.socialmedia.auth.domain.User;

public interface EmailVerificationService {

    void sendVerification(User user);

    void resendVerification(String email);

    void verify(String rawToken);
}
