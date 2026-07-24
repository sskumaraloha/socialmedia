package com.socialmedia.auth.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;

import com.socialmedia.auth.domain.AuthProvider;
import com.socialmedia.auth.domain.User;
import com.socialmedia.auth.dto.response.TwoFactorSetupResponse;
import com.socialmedia.auth.event.AuthEventPublisher;
import com.socialmedia.auth.repository.UserRepository;
import com.socialmedia.common.exception.BusinessException;
import dev.samstevens.totp.code.DefaultCodeGenerator;
import dev.samstevens.totp.time.SystemTimeProvider;
import java.lang.reflect.Field;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TwoFactorAuthServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private AuthEventPublisher eventPublisher;

    private TwoFactorAuthServiceImpl twoFactorAuthService;
    private User user;

    @BeforeEach
    void setUp() throws Exception {
        twoFactorAuthService = new TwoFactorAuthServiceImpl(userRepository, eventPublisher);
        user = new User("user@example.com", "hash", AuthProvider.LOCAL, null);
        setId(user, UUID.randomUUID());
        lenient().when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void beginSetupGeneratesASecretAndQrCode() {
        TwoFactorSetupResponse response = twoFactorAuthService.beginSetup(user);

        assertThat(response.secret()).isNotBlank();
        assertThat(response.otpAuthUrl()).startsWith("otpauth://totp/");
        assertThat(response.qrCodeImageBase64()).startsWith("data:image/png;base64,");
        assertThat(user.getTwoFactorSecret()).isEqualTo(response.secret());
        assertThat(user.isTwoFactorEnabled()).isFalse();
    }

    @Test
    void verifyAndEnableSucceedsWithAValidCode() throws Exception {
        TwoFactorSetupResponse setup = twoFactorAuthService.beginSetup(user);
        String validCode = new DefaultCodeGenerator().generate(setup.secret(),
                new SystemTimeProvider().getTime() / 30);

        twoFactorAuthService.verifyAndEnable(user, validCode);

        assertThat(user.isTwoFactorEnabled()).isTrue();
        verify(eventPublisher).publishTwoFactorChanged(user.getId(), true);
    }

    @Test
    void verifyAndEnableRejectsAnInvalidCode() {
        twoFactorAuthService.beginSetup(user);

        assertThatThrownBy(() -> twoFactorAuthService.verifyAndEnable(user, "000000"))
                .isInstanceOf(BusinessException.class);
        assertThat(user.isTwoFactorEnabled()).isFalse();
    }

    @Test
    void verifyAndEnableRejectsWhenSetupNeverStarted() {
        assertThatThrownBy(() -> twoFactorAuthService.verifyAndEnable(user, "123456"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("setup");
    }

    private static void setId(User user, UUID id) throws Exception {
        Field idField = Class.forName("com.socialmedia.auth.domain.BaseEntity").getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(user, id);
    }
}
