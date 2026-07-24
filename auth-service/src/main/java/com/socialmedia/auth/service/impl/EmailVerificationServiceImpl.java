package com.socialmedia.auth.service.impl;

import com.socialmedia.auth.domain.EmailVerificationToken;
import com.socialmedia.auth.domain.User;
import com.socialmedia.auth.event.AuthEventPublisher;
import com.socialmedia.auth.repository.EmailVerificationTokenRepository;
import com.socialmedia.auth.repository.UserRepository;
import com.socialmedia.auth.security.TokenHasher;
import com.socialmedia.auth.service.EmailVerificationService;
import com.socialmedia.common.exception.BusinessException;
import com.socialmedia.common.exception.ResourceNotFoundException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EmailVerificationServiceImpl implements EmailVerificationService {

    private static final long TOKEN_TTL_HOURS = 24;

    private final EmailVerificationTokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final AuthEventPublisher eventPublisher;

    public EmailVerificationServiceImpl(EmailVerificationTokenRepository tokenRepository,
            UserRepository userRepository, AuthEventPublisher eventPublisher) {
        this.tokenRepository = tokenRepository;
        this.userRepository = userRepository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    @Transactional
    public void sendVerification(User user) {
        String rawToken = TokenHasher.generateRawToken();
        EmailVerificationToken token = new EmailVerificationToken(user, TokenHasher.hash(rawToken),
                Instant.now().plus(TOKEN_TTL_HOURS, ChronoUnit.HOURS));
        tokenRepository.save(token);
        eventPublisher.publishEmailVerificationRequested(user.getId(), user.getEmail(), rawToken);
    }

    @Override
    @Transactional
    public void resendVerification(String email) {
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", email));
        if (user.isEmailVerified()) {
            throw new BusinessException(HttpStatus.CONFLICT, "EMAIL_ALREADY_VERIFIED", "Email is already verified");
        }
        sendVerification(user);
    }

    @Override
    @Transactional
    public void verify(String rawToken) {
        EmailVerificationToken token = tokenRepository.findByTokenHash(TokenHasher.hash(rawToken))
                .orElseThrow(() -> new BusinessException(HttpStatus.BAD_REQUEST, "INVALID_VERIFICATION_TOKEN",
                        "Verification link is invalid or has already been used"));
        if (!token.isValid()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "EXPIRED_VERIFICATION_TOKEN",
                    "Verification link has expired - request a new one");
        }
        token.setUsed(true);
        User user = token.getUser();
        user.setEmailVerified(true);
        userRepository.save(user);
    }
}
