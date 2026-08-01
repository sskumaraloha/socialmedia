package com.socialmedia.message.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.socialmedia.message.domain.EncryptedEnvelope;
import com.socialmedia.message.dto.request.EncryptedEnvelopeRequest;
import java.util.Base64;
import java.util.List;
import org.junit.jupiter.api.Test;

class EncryptedPayloadValidatorTest {

    private final EncryptedPayloadValidator validator = new EncryptedPayloadValidator(1024, 4);

    private static String b64(String s) {
        return Base64.getEncoder().encodeToString(s.getBytes());
    }

    @Test
    void passesThroughWellFormedEnvelopesWithoutAlteringTheCiphertext() {
        List<EncryptedEnvelope> result = validator.validateAndConvert(List.of(
                new EncryptedEnvelopeRequest("device-a", 3, b64("cipher-a")),
                new EncryptedEnvelopeRequest("device-b", 2, b64("cipher-b"))));

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getRecipientDeviceId()).isEqualTo("device-a");
        assertThat(result.get(0).getCipherType()).isEqualTo(3);
        assertThat(result.get(0).getCiphertext()).isEqualTo(b64("cipher-a"));
        assertThat(result.get(1).getCiphertext()).isEqualTo(b64("cipher-b"));
    }

    @Test
    void rejectsAnEmptyEnvelopeList() {
        assertThatThrownBy(() -> validator.validateAndConvert(List.of()))
                .hasMessageContaining("cannot encrypt on your behalf");
    }

    @Test
    void rejectsNullEnvelopeList() {
        assertThatThrownBy(() -> validator.validateAndConvert(null))
                .hasMessageContaining("cannot encrypt on your behalf");
    }

    @Test
    void rejectsTwoEnvelopesTargetingTheSameDevice() {
        assertThatThrownBy(() -> validator.validateAndConvert(List.of(
                new EncryptedEnvelopeRequest("device-a", 3, b64("one")),
                new EncryptedEnvelopeRequest("device-a", 3, b64("two")))))
                .hasMessageContaining("same device");
    }

    @Test
    void rejectsAnUnknownCipherType() {
        assertThatThrownBy(() -> validator.validateAndConvert(List.of(
                new EncryptedEnvelopeRequest("device-a", 99, b64("cipher")))))
                .hasMessageContaining("Unsupported cipher type");
    }

    @Test
    void rejectsNonBase64Ciphertext() {
        assertThatThrownBy(() -> validator.validateAndConvert(List.of(
                new EncryptedEnvelopeRequest("device-a", 3, "!!!not base64!!!"))))
                .hasMessageContaining("valid Base64");
    }

    @Test
    void rejectsEmptyCiphertext() {
        assertThatThrownBy(() -> validator.validateAndConvert(List.of(
                new EncryptedEnvelopeRequest("device-a", 3, ""))))
                .hasMessageContaining("must not be empty");
    }

    @Test
    void rejectsCiphertextOverTheConfiguredSizeCap() {
        String oversized = Base64.getEncoder().encodeToString(new byte[2048]);
        assertThatThrownBy(() -> validator.validateAndConvert(List.of(
                new EncryptedEnvelopeRequest("device-a", 3, oversized))))
                .hasMessageContaining("exceeds the maximum");
    }

    @Test
    void rejectsMoreEnvelopesThanTheConfiguredDeviceFanOutCap() {
        assertThatThrownBy(() -> validator.validateAndConvert(List.of(
                new EncryptedEnvelopeRequest("d1", 3, b64("a")),
                new EncryptedEnvelopeRequest("d2", 3, b64("b")),
                new EncryptedEnvelopeRequest("d3", 3, b64("c")),
                new EncryptedEnvelopeRequest("d4", 3, b64("d")),
                new EncryptedEnvelopeRequest("d5", 3, b64("e")))))
                .hasMessageContaining("at most 4 devices");
    }
}
