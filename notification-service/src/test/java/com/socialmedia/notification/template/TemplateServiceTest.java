package com.socialmedia.notification.template;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.socialmedia.notification.exception.UnknownNotificationTemplateException;
import java.util.Map;
import org.junit.jupiter.api.Test;

class TemplateServiceTest {

    private final TemplateService service = new TemplateService();

    @Test
    void rendersTheDefaultEnglishTemplateWithSubstitutedParams() {
        TemplateResult result = service.render("email-verification", null, Map.of("token", "ABC123"));

        assertThat(result.subject()).isEqualTo("Verify your email address");
        assertThat(result.body()).contains("ABC123");
    }

    @Test
    void fallsBackToEnglishForAnUnsupportedLocale() {
        TemplateResult result = service.render("password-reset", "fr", Map.of("token", "XYZ"));

        assertThat(result.subject()).isEqualTo("Reset your password");
    }

    @Test
    void rendersTheSpanishTemplateWhenRequested() {
        TemplateResult result = service.render("password-reset", "es", Map.of("token", "XYZ"));

        assertThat(result.subject()).isEqualTo("Restablece tu contraseña");
        assertThat(result.body()).contains("XYZ");
    }

    @Test
    void substitutesMultiplePlaceholders() {
        TemplateResult result = service.render("new-message", null, Map.of("senderName", "Alice", "preview", "hi there"));

        assertThat(result.subject()).isEqualTo("New message from Alice");
        assertThat(result.body()).isEqualTo("You have a new message from Alice: hi there");
    }

    @Test
    void unknownTemplateKeyThrows() {
        assertThatThrownBy(() -> service.render("does-not-exist", null, Map.of()))
                .isInstanceOf(UnknownNotificationTemplateException.class);
    }
}
