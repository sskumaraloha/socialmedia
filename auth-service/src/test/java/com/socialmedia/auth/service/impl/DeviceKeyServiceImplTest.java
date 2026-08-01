package com.socialmedia.auth.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.socialmedia.auth.domain.Device;
import com.socialmedia.auth.domain.DeviceIdentityKey;
import com.socialmedia.auth.domain.DeviceType;
import com.socialmedia.auth.domain.OneTimePreKey;
import com.socialmedia.auth.domain.User;
import com.socialmedia.auth.dto.request.UploadKeyBundleRequest;
import com.socialmedia.auth.dto.response.KeyBundleResponse;
import com.socialmedia.auth.dto.response.PreKeyCountResponse;
import com.socialmedia.auth.repository.DeviceIdentityKeyRepository;
import com.socialmedia.auth.repository.DeviceRepository;
import com.socialmedia.auth.repository.OneTimePreKeyRepository;
import com.socialmedia.common.exception.BusinessException;
import com.socialmedia.common.exception.ForbiddenException;
import com.socialmedia.common.lock.RedisDistributedLock;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

@ExtendWith(MockitoExtension.class)
class DeviceKeyServiceImplTest {

    @Mock private DeviceRepository deviceRepository;
    @Mock private DeviceIdentityKeyRepository deviceIdentityKeyRepository;
    @Mock private OneTimePreKeyRepository oneTimePreKeyRepository;
    @Mock private StringRedisTemplate redisTemplate;
    @Mock private ValueOperations<String, String> valueOperations;

    private DeviceKeyServiceImpl service;
    private User user;
    private Device device;

    @BeforeEach
    void setUp() {
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        lenient().when(valueOperations.setIfAbsent(any(), any(), any(Duration.class))).thenReturn(true);
        RedisDistributedLock distributedLock = new RedisDistributedLock(redisTemplate);
        service = new DeviceKeyServiceImpl(deviceRepository, deviceIdentityKeyRepository, oneTimePreKeyRepository, distributedLock);

        user = new User("alice@example.com", "hash", com.socialmedia.auth.domain.AuthProvider.LOCAL, null);
        setId(user, UUID.randomUUID());
        device = new Device(user, "device-1", "Alice's Phone", DeviceType.ANDROID);
    }

    private static void setId(User user, UUID id) {
        try {
            var field = com.socialmedia.common.jpa.BaseEntity.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(user, id);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    private static String b64(String s) {
        return Base64.getEncoder().encodeToString(s.getBytes());
    }

    @Test
    void uploadKeyBundleCreatesANewBundleWhenNoneExists() {
        when(deviceRepository.findByDeviceId("device-1")).thenReturn(Optional.of(device));
        when(deviceIdentityKeyRepository.findByDeviceId("device-1")).thenReturn(Optional.empty());
        when(oneTimePreKeyRepository.findByDeviceIdAndKeyId("device-1", 1)).thenReturn(Optional.empty());

        UploadKeyBundleRequest request = new UploadKeyBundleRequest(1234, b64("identity"), 5, b64("signed-prekey"),
                b64("signature"), 9, b64("kyber-prekey"), b64("kyber-sig"),
                List.of(new UploadKeyBundleRequest.OneTimePreKeyUpload(1, b64("otpk-1"))));

        service.uploadKeyBundle(user, "device-1", request);

        verify(deviceIdentityKeyRepository).save(any(DeviceIdentityKey.class));
        verify(oneTimePreKeyRepository).save(any(OneTimePreKey.class));
    }

    @Test
    void uploadKeyBundleRejectsAChangedIdentityKeyOnRotation() {
        when(deviceRepository.findByDeviceId("device-1")).thenReturn(Optional.of(device));
        DeviceIdentityKey existing = new DeviceIdentityKey("device-1", 1234, "original-identity".getBytes(), 1,
                "old-signed".getBytes(), "old-sig".getBytes(), 9, "kyber".getBytes(), "kyber-sig".getBytes());
        when(deviceIdentityKeyRepository.findByDeviceId("device-1")).thenReturn(Optional.of(existing));

        UploadKeyBundleRequest request = new UploadKeyBundleRequest(1234, b64("different-identity"), 2, b64("new-signed"),
                b64("new-sig"), 9, b64("kyber-prekey"), b64("kyber-sig"),
                List.of(new UploadKeyBundleRequest.OneTimePreKeyUpload(1, b64("otpk-1"))));

        assertThatThrownBy(() -> service.uploadKeyBundle(user, "device-1", request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("different registered identity key");
    }

    @Test
    void uploadKeyBundleRejectsADeviceThatDoesNotBelongToTheCaller() {
        User otherUser = new User("bob@example.com", "hash", com.socialmedia.auth.domain.AuthProvider.LOCAL, null);
        setId(otherUser, UUID.randomUUID());
        when(deviceRepository.findByDeviceId("device-1")).thenReturn(Optional.of(device));

        UploadKeyBundleRequest request = new UploadKeyBundleRequest(1234, b64("identity"), 1, b64("signed"), b64("sig"),
                9, b64("kyber-prekey"), b64("kyber-sig"),
                List.of(new UploadKeyBundleRequest.OneTimePreKeyUpload(1, b64("otpk-1"))));

        assertThatThrownBy(() -> service.uploadKeyBundle(otherUser, "device-1", request))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void getKeyBundleClaimsAndConsumesOneOneTimePreKey() {
        DeviceIdentityKey identityKey = new DeviceIdentityKey("device-1", 1234, "identity".getBytes(), 5,
                "signed-prekey".getBytes(), "signature".getBytes(), 9, "kyber".getBytes(), "kyber-sig".getBytes());
        when(deviceIdentityKeyRepository.findByDeviceId("device-1")).thenReturn(Optional.of(identityKey));
        OneTimePreKey otpk = new OneTimePreKey("device-1", 1, "otpk-public".getBytes());
        when(oneTimePreKeyRepository.findFirstByDeviceIdAndConsumedFalseOrderByCreatedAtAsc("device-1"))
                .thenReturn(Optional.of(otpk));

        KeyBundleResponse response = service.getKeyBundle("device-1");

        assertThat(response.deviceId()).isEqualTo("device-1");
        assertThat(response.oneTimePreKeyId()).isEqualTo(1);
        assertThat(otpk.isConsumed()).isTrue();
        verify(oneTimePreKeyRepository).save(otpk);
    }

    @Test
    void getKeyBundleReturnsNullOneTimePreKeyWhenPoolIsExhausted() {
        DeviceIdentityKey identityKey = new DeviceIdentityKey("device-1", 1234, "identity".getBytes(), 5,
                "signed-prekey".getBytes(), "signature".getBytes(), 9, "kyber".getBytes(), "kyber-sig".getBytes());
        when(deviceIdentityKeyRepository.findByDeviceId("device-1")).thenReturn(Optional.of(identityKey));
        when(oneTimePreKeyRepository.findFirstByDeviceIdAndConsumedFalseOrderByCreatedAtAsc("device-1"))
                .thenReturn(Optional.empty());

        KeyBundleResponse response = service.getKeyBundle("device-1");

        assertThat(response.oneTimePreKeyId()).isNull();
        assertThat(response.oneTimePreKeyPublic()).isNull();
        verify(oneTimePreKeyRepository, never()).save(any());
    }

    @Test
    void getKeyBundleThrowsWhenTheDeviceHasNeverPublishedKeys() {
        when(deviceIdentityKeyRepository.findByDeviceId("device-1")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getKeyBundle("device-1"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("has not published");
    }

    @Test
    void getPreKeyCountReturnsTheUnconsumedCountForOwnDevice() {
        when(deviceRepository.findByDeviceId("device-1")).thenReturn(Optional.of(device));
        when(oneTimePreKeyRepository.countByDeviceIdAndConsumedFalse("device-1")).thenReturn(7L);

        PreKeyCountResponse response = service.getPreKeyCount(user, "device-1");

        assertThat(response.remainingOneTimePreKeys()).isEqualTo(7L);
    }
}
