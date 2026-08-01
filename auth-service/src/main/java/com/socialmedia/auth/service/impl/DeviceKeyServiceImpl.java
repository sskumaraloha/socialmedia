package com.socialmedia.auth.service.impl;

import com.socialmedia.auth.domain.Device;
import com.socialmedia.auth.domain.DeviceIdentityKey;
import com.socialmedia.auth.domain.OneTimePreKey;
import com.socialmedia.auth.domain.User;
import com.socialmedia.auth.dto.request.UploadKeyBundleRequest;
import com.socialmedia.auth.dto.response.KeyBundleResponse;
import com.socialmedia.auth.dto.response.PreKeyCountResponse;
import com.socialmedia.auth.repository.DeviceIdentityKeyRepository;
import com.socialmedia.auth.repository.DeviceRepository;
import com.socialmedia.auth.repository.OneTimePreKeyRepository;
import com.socialmedia.auth.service.DeviceKeyService;
import com.socialmedia.common.exception.BusinessException;
import com.socialmedia.common.exception.ForbiddenException;
import com.socialmedia.common.exception.ResourceNotFoundException;
import com.socialmedia.common.lock.RedisDistributedLock;
import java.time.Duration;
import java.util.Arrays;
import java.util.Base64;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Deliberately never imports any crypto library - this is a dumb public-key directory, exactly
 * like Signal's own server. Every value in and out is an opaque Base64 byte blob; the actual
 * X3DH key agreement and Double Ratchet encryption happen entirely on the client (see
 * message-service and this platform's Phase 1 verification harness for where a real client's
 * crypto layer - libsignal-client, in the reference test - actually uses these bundles).
 */
@Service
public class DeviceKeyServiceImpl implements DeviceKeyService {

    private final DeviceRepository deviceRepository;
    private final DeviceIdentityKeyRepository deviceIdentityKeyRepository;
    private final OneTimePreKeyRepository oneTimePreKeyRepository;
    private final RedisDistributedLock distributedLock;

    public DeviceKeyServiceImpl(DeviceRepository deviceRepository, DeviceIdentityKeyRepository deviceIdentityKeyRepository,
            OneTimePreKeyRepository oneTimePreKeyRepository, RedisDistributedLock distributedLock) {
        this.deviceRepository = deviceRepository;
        this.deviceIdentityKeyRepository = deviceIdentityKeyRepository;
        this.oneTimePreKeyRepository = oneTimePreKeyRepository;
        this.distributedLock = distributedLock;
    }

    @Override
    @Transactional
    public void uploadKeyBundle(User user, String deviceId, UploadKeyBundleRequest request) {
        requireOwnDevice(user, deviceId);

        byte[] identityPublicKey = decode(request.identityPublicKey());
        byte[] signedPreKeyPublic = decode(request.signedPreKeyPublic());
        byte[] signedPreKeySignature = decode(request.signedPreKeySignature());
        byte[] kyberPreKeyPublic = decode(request.kyberPreKeyPublic());
        byte[] kyberPreKeySignature = decode(request.kyberPreKeySignature());

        DeviceIdentityKey existing = deviceIdentityKeyRepository.findByDeviceId(deviceId).orElse(null);
        if (existing == null) {
            deviceIdentityKeyRepository.save(new DeviceIdentityKey(deviceId, request.registrationId(), identityPublicKey,
                    request.signedPreKeyId(), signedPreKeyPublic, signedPreKeySignature,
                    request.kyberPreKeyId(), kyberPreKeyPublic, kyberPreKeySignature));
        } else {
            // The identity key is the one thing that must NOT silently change here - a real
            // client would show a "safety number changed" warning if a contact's identity key
            // changes underneath it. Rejecting it here (rather than accepting a mismatched key)
            // means such a change can only happen deliberately (revoke + re-add the device).
            if (!Arrays.equals(existing.getIdentityPublicKey(), identityPublicKey)) {
                throw new BusinessException(HttpStatus.CONFLICT, "IDENTITY_KEY_MISMATCH",
                        "This device already has a different registered identity key - revoke and re-add the device to change it");
            }
            existing.rotatePreKeys(request.signedPreKeyId(), signedPreKeyPublic, signedPreKeySignature,
                    request.kyberPreKeyId(), kyberPreKeyPublic, kyberPreKeySignature);
        }

        for (UploadKeyBundleRequest.OneTimePreKeyUpload upload : request.oneTimePreKeys()) {
            if (oneTimePreKeyRepository.findByDeviceIdAndKeyId(deviceId, upload.keyId()).isEmpty()) {
                oneTimePreKeyRepository.save(new OneTimePreKey(deviceId, upload.keyId(), decode(upload.publicKey())));
            }
        }
    }

    @Override
    @Transactional
    public KeyBundleResponse getKeyBundle(String deviceId) {
        DeviceIdentityKey identityKey = deviceIdentityKeyRepository.findByDeviceId(deviceId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "NO_KEY_BUNDLE",
                        "This device has not published any encryption keys"));

        // Two callers racing to start a session with the same device must never be handed the
        // same one-time prekey - reusing this exact RedisDistributedLock, first built for
        // auth-service's own registration race, for the same reason: a plain
        // read-then-mark-consumed has a window two concurrent requests can both pass through.
        return distributedLock.withLock("prekey-claim:" + deviceId, Duration.ofSeconds(5), Duration.ofSeconds(3), () -> {
            OneTimePreKey claimed = oneTimePreKeyRepository
                    .findFirstByDeviceIdAndConsumedFalseOrderByCreatedAtAsc(deviceId).orElse(null);
            if (claimed != null) {
                claimed.markConsumed();
                oneTimePreKeyRepository.save(claimed);
            }
            return new KeyBundleResponse(deviceId, identityKey.getRegistrationId(),
                    encode(identityKey.getIdentityPublicKey()),
                    identityKey.getSignedPreKeyId(), encode(identityKey.getSignedPreKeyPublic()),
                    encode(identityKey.getSignedPreKeySignature()),
                    identityKey.getKyberPreKeyId(), encode(identityKey.getKyberPreKeyPublic()),
                    encode(identityKey.getKyberPreKeySignature()),
                    claimed == null ? null : claimed.getKeyId(),
                    claimed == null ? null : encode(claimed.getPublicKey()));
        });
    }

    @Override
    public PreKeyCountResponse getPreKeyCount(User user, String deviceId) {
        requireOwnDevice(user, deviceId);
        return new PreKeyCountResponse(deviceId, oneTimePreKeyRepository.countByDeviceIdAndConsumedFalse(deviceId));
    }

    private Device requireOwnDevice(User user, String deviceId) {
        Device device = deviceRepository.findByDeviceId(deviceId)
                .orElseThrow(() -> new ResourceNotFoundException("Device", deviceId));
        if (!device.getUser().getId().equals(user.getId())) {
            throw new ForbiddenException("This device does not belong to you");
        }
        if (device.isRevoked()) {
            throw new BusinessException(HttpStatus.GONE, "DEVICE_REVOKED", "This device has been revoked");
        }
        return device;
    }

    private static byte[] decode(String base64) {
        try {
            return Base64.getDecoder().decode(base64);
        } catch (IllegalArgumentException e) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "INVALID_KEY_ENCODING", "Key material must be valid Base64");
        }
    }

    private static String encode(byte[] bytes) {
        return Base64.getEncoder().encodeToString(bytes);
    }
}
