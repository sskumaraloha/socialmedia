package com.socialmedia.auth.controller;

import com.socialmedia.auth.domain.User;
import com.socialmedia.auth.dto.request.ChangePasswordRequest;
import com.socialmedia.auth.dto.request.ForgotPasswordRequest;
import com.socialmedia.auth.dto.request.LoginRequest;
import com.socialmedia.auth.dto.request.RefreshTokenRequest;
import com.socialmedia.auth.dto.request.RegisterRequest;
import com.socialmedia.auth.dto.request.ResendVerificationRequest;
import com.socialmedia.auth.dto.request.ResetPasswordRequest;
import com.socialmedia.auth.dto.request.TwoFactorLoginRequest;
import com.socialmedia.auth.dto.request.VerifyEmailRequest;
import com.socialmedia.auth.dto.response.AuthTokenResponse;
import com.socialmedia.auth.dto.response.LoginHistoryResponse;
import com.socialmedia.auth.dto.response.LoginResult;
import com.socialmedia.auth.dto.response.MessageResponse;
import com.socialmedia.auth.dto.response.UserSummaryResponse;
import com.socialmedia.auth.exception.InvalidCredentialsException;
import com.socialmedia.auth.mapper.UserMapper;
import com.socialmedia.auth.repository.UserRepository;
import com.socialmedia.auth.security.ClientIpResolver;
import com.socialmedia.auth.security.CurrentUserProvider;
import com.socialmedia.auth.service.AuthService;
import com.socialmedia.auth.service.EmailVerificationService;
import com.socialmedia.auth.service.LoginHistoryService;
import com.socialmedia.auth.service.PasswordResetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication", description = "Registration, login, tokens, password/email flows")
public class AuthController {

    private final AuthService authService;
    private final EmailVerificationService emailVerificationService;
    private final PasswordResetService passwordResetService;
    private final LoginHistoryService loginHistoryService;
    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final CurrentUserProvider currentUserProvider;

    public AuthController(AuthService authService, EmailVerificationService emailVerificationService,
            PasswordResetService passwordResetService, LoginHistoryService loginHistoryService,
            UserRepository userRepository, UserMapper userMapper, PasswordEncoder passwordEncoder,
            CurrentUserProvider currentUserProvider) {
        this.authService = authService;
        this.emailVerificationService = emailVerificationService;
        this.passwordResetService = passwordResetService;
        this.loginHistoryService = loginHistoryService;
        this.userRepository = userRepository;
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.currentUserProvider = currentUserProvider;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a new local account and log in")
    public AuthTokenResponse register(@Valid @RequestBody RegisterRequest request, HttpServletRequest httpRequest) {
        return authService.register(request, ClientIpResolver.resolve(httpRequest), httpRequest.getHeader("User-Agent"));
    }

    @PostMapping("/login")
    @Operation(summary = "Log in with email + password",
            description = "Returns an AuthTokenResponse for accounts without 2FA (HTTP 200), or a "
                    + "TwoFactorChallengeResponse (HTTP 428) for 2FA-enabled accounts - present its "
                    + "challengeToken and a TOTP code to POST /login/2fa to finish.")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        LoginResult result = authService.login(request, ClientIpResolver.resolve(httpRequest),
                httpRequest.getHeader("User-Agent"));
        if (result.requiresTwoFactor()) {
            return ResponseEntity.status(HttpStatus.PRECONDITION_REQUIRED).body(result.twoFactorChallenge());
        }
        return ResponseEntity.ok(result.tokens());
    }

    @PostMapping("/login/2fa")
    @Operation(summary = "Complete a login that returned a 2FA challenge")
    public AuthTokenResponse completeTwoFactorLogin(@Valid @RequestBody TwoFactorLoginRequest request,
            HttpServletRequest httpRequest) {
        return authService.completeTwoFactorLogin(request.challengeToken(), request.code(),
                ClientIpResolver.resolve(httpRequest), httpRequest.getHeader("User-Agent"));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Exchange a refresh token for a new access/refresh token pair (rotates the refresh token)")
    public AuthTokenResponse refresh(@Valid @RequestBody RefreshTokenRequest request, HttpServletRequest httpRequest) {
        return authService.refresh(request.refreshToken(), ClientIpResolver.resolve(httpRequest),
                httpRequest.getHeader("User-Agent"));
    }

    @PostMapping("/logout")
    @Operation(summary = "Log out the current session/device")
    public MessageResponse logout(@Valid @RequestBody RefreshTokenRequest request) {
        authService.logout(request.refreshToken(), currentUserProvider.jti().orElse(null));
        return new MessageResponse("Logged out");
    }

    @PostMapping("/logout-all")
    @Operation(summary = "Log out of every device - use after a suspected compromise")
    public MessageResponse logoutAll() {
        authService.logoutAllDevices(currentUserProvider.get());
        return new MessageResponse("Logged out of all devices");
    }

    @GetMapping("/me")
    @Operation(summary = "Current authenticated user")
    public UserSummaryResponse me() {
        return userMapper.toSummary(currentUserProvider.get());
    }

    @PostMapping("/change-password")
    @Operation(summary = "Change password while authenticated (requires the current password)")
    public MessageResponse changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        User user = currentUserProvider.get();
        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
        return new MessageResponse("Password changed");
    }

    @PostMapping("/verify-email")
    @Operation(summary = "Verify email using the token from the verification link")
    public MessageResponse verifyEmail(@Valid @RequestBody VerifyEmailRequest request) {
        emailVerificationService.verify(request.token());
        return new MessageResponse("Email verified");
    }

    @PostMapping("/resend-verification")
    @Operation(summary = "Resend the email verification link")
    public MessageResponse resendVerification(@Valid @RequestBody ResendVerificationRequest request) {
        emailVerificationService.resendVerification(request.email());
        return new MessageResponse("Verification email sent if the account exists and isn't already verified");
    }

    @PostMapping("/forgot-password")
    @Operation(summary = "Request a password reset link")
    public MessageResponse forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        passwordResetService.requestReset(request.email());
        return new MessageResponse("Password reset email sent if the account exists");
    }

    @PostMapping("/reset-password")
    @Operation(summary = "Reset password using the token from the reset link")
    public MessageResponse resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        passwordResetService.resetPassword(request.token(), request.newPassword());
        return new MessageResponse("Password reset - please log in again");
    }

    @GetMapping("/login-history")
    @Operation(summary = "Recent login attempts for the current account")
    public Page<LoginHistoryResponse> loginHistory(Pageable pageable) {
        return loginHistoryService.list(currentUserProvider.get(), pageable);
    }

    @DeleteMapping("/me")
    @Operation(summary = "Deactivate the current account (soft delete)")
    public MessageResponse deactivateAccount() {
        User user = currentUserProvider.get();
        user.setEnabled(false);
        userRepository.save(user);
        authService.logoutAllDevices(user);
        return new MessageResponse("Account deactivated");
    }
}
