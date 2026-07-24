package com.socialmedia.auth.controller;

import com.socialmedia.auth.dto.request.TwoFactorVerifyRequest;
import com.socialmedia.auth.dto.response.MessageResponse;
import com.socialmedia.auth.dto.response.TwoFactorSetupResponse;
import com.socialmedia.auth.security.CurrentUserProvider;
import com.socialmedia.auth.service.TwoFactorAuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth/2fa")
@Tag(name = "Two-Factor Authentication", description = "TOTP-based 2FA setup and management")
public class TwoFactorController {

    private final TwoFactorAuthService twoFactorAuthService;
    private final CurrentUserProvider currentUserProvider;

    public TwoFactorController(TwoFactorAuthService twoFactorAuthService, CurrentUserProvider currentUserProvider) {
        this.twoFactorAuthService = twoFactorAuthService;
        this.currentUserProvider = currentUserProvider;
    }

    @PostMapping("/setup")
    @Operation(summary = "Generate a new TOTP secret + QR code (not yet enabled)")
    public TwoFactorSetupResponse setup() {
        return twoFactorAuthService.beginSetup(currentUserProvider.get());
    }

    @PostMapping("/verify")
    @Operation(summary = "Confirm a code from the authenticator app to enable 2FA")
    public MessageResponse verify(@Valid @RequestBody TwoFactorVerifyRequest request) {
        twoFactorAuthService.verifyAndEnable(currentUserProvider.get(), request.code());
        return new MessageResponse("Two-factor authentication enabled");
    }

    @PostMapping("/disable")
    @Operation(summary = "Disable 2FA (requires a valid current code)")
    public MessageResponse disable(@Valid @RequestBody TwoFactorVerifyRequest request) {
        twoFactorAuthService.disable(currentUserProvider.get(), request.code());
        return new MessageResponse("Two-factor authentication disabled");
    }
}
