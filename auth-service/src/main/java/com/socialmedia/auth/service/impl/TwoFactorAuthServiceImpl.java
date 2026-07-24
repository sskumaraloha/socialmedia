package com.socialmedia.auth.service.impl;

import com.socialmedia.auth.domain.User;
import com.socialmedia.auth.dto.response.TwoFactorSetupResponse;
import com.socialmedia.auth.event.AuthEventPublisher;
import com.socialmedia.auth.repository.UserRepository;
import com.socialmedia.auth.service.TwoFactorAuthService;
import com.socialmedia.common.exception.BusinessException;
import dev.samstevens.totp.code.CodeVerifier;
import dev.samstevens.totp.code.DefaultCodeGenerator;
import dev.samstevens.totp.code.DefaultCodeVerifier;
import dev.samstevens.totp.code.HashingAlgorithm;
import dev.samstevens.totp.exceptions.QrGenerationException;
import dev.samstevens.totp.qr.QrData;
import dev.samstevens.totp.qr.QrGenerator;
import dev.samstevens.totp.qr.ZxingPngQrGenerator;
import dev.samstevens.totp.secret.DefaultSecretGenerator;
import dev.samstevens.totp.secret.SecretGenerator;
import dev.samstevens.totp.time.SystemTimeProvider;
import java.util.Base64;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TwoFactorAuthServiceImpl implements TwoFactorAuthService {

    private static final String ISSUER = "SocialMedia Platform";

    private final UserRepository userRepository;
    private final AuthEventPublisher eventPublisher;
    private final SecretGenerator secretGenerator = new DefaultSecretGenerator();
    private final QrGenerator qrGenerator = new ZxingPngQrGenerator();
    private final CodeVerifier codeVerifier = new DefaultCodeVerifier(new DefaultCodeGenerator(), new SystemTimeProvider());

    public TwoFactorAuthServiceImpl(UserRepository userRepository, AuthEventPublisher eventPublisher) {
        this.userRepository = userRepository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    @Transactional
    public TwoFactorSetupResponse beginSetup(User user) {
        String secret = secretGenerator.generate();
        // Not enabled yet - persisted as a pending secret until verifyAndEnable() confirms
        // the user actually has it in their authenticator app.
        user.setTwoFactorSecret(secret);
        userRepository.save(user);

        QrData data = new QrData.Builder()
                .label(user.getEmail())
                .secret(secret)
                .issuer(ISSUER)
                .algorithm(HashingAlgorithm.SHA1)
                .digits(6)
                .period(30)
                .build();

        String qrCodeBase64;
        try {
            byte[] imageBytes = qrGenerator.generate(data);
            qrCodeBase64 = "data:" + qrGenerator.getImageMimeType() + ";base64,"
                    + Base64.getEncoder().encodeToString(imageBytes);
        } catch (QrGenerationException e) {
            throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "QR_GENERATION_FAILED",
                    "Failed to generate 2FA QR code");
        }

        return new TwoFactorSetupResponse(secret, data.getUri(), qrCodeBase64);
    }

    @Override
    @Transactional
    public void verifyAndEnable(User user, String code) {
        if (user.getTwoFactorSecret() == null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "TWO_FACTOR_SETUP_NOT_STARTED",
                    "Call the setup endpoint before verifying a code");
        }
        if (!isCodeValid(user.getTwoFactorSecret(), code)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "INVALID_TOTP_CODE", "Invalid authentication code");
        }
        user.setTwoFactorEnabled(true);
        userRepository.save(user);
        eventPublisher.publishTwoFactorChanged(user.getId(), true);
    }

    @Override
    @Transactional
    public void disable(User user, String code) {
        if (!user.isTwoFactorEnabled() || !isCodeValid(user.getTwoFactorSecret(), code)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "INVALID_TOTP_CODE", "Invalid authentication code");
        }
        user.setTwoFactorEnabled(false);
        user.setTwoFactorSecret(null);
        userRepository.save(user);
        eventPublisher.publishTwoFactorChanged(user.getId(), false);
    }

    @Override
    public boolean isCodeValid(String secret, String code) {
        return secret != null && codeVerifier.isValidCode(secret, code);
    }
}
