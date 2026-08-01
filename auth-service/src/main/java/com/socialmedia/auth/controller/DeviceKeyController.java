package com.socialmedia.auth.controller;

import com.socialmedia.auth.dto.request.UploadKeyBundleRequest;
import com.socialmedia.auth.dto.response.KeyBundleResponse;
import com.socialmedia.auth.dto.response.PreKeyCountResponse;
import com.socialmedia.auth.security.CurrentUserProvider;
import com.socialmedia.auth.service.DeviceKeyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public key bundle directory for end-to-end encryption - see DeviceKeyServiceImpl's javadoc
 * for why this never touches a crypto library or private key material.
 */
@RestController
@RequestMapping("/api/v1/auth/devices")
@Tag(name = "Device Encryption Keys", description = "Publish and fetch Signal-protocol-style public key bundles for end-to-end encryption")
public class DeviceKeyController {

    private final DeviceKeyService deviceKeyService;
    private final CurrentUserProvider currentUserProvider;

    public DeviceKeyController(DeviceKeyService deviceKeyService, CurrentUserProvider currentUserProvider) {
        this.deviceKeyService = deviceKeyService;
        this.currentUserProvider = currentUserProvider;
    }

    @PutMapping("/{deviceId}/keys")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Upload or rotate this device's public key bundle (identity key, signed prekey, one-time prekeys)")
    public void uploadKeyBundle(@PathVariable String deviceId, @Valid @RequestBody UploadKeyBundleRequest request) {
        deviceKeyService.uploadKeyBundle(currentUserProvider.get(), deviceId, request);
    }

    @GetMapping("/{deviceId}/key-bundle")
    @Operation(summary = "Fetch a device's public key bundle to initiate an encrypted session with it")
    public KeyBundleResponse getKeyBundle(@PathVariable String deviceId) {
        return deviceKeyService.getKeyBundle(deviceId);
    }

    @GetMapping("/{deviceId}/prekeys/count")
    @Operation(summary = "Count of this (own) device's unused one-time prekeys, to know when to top up")
    public PreKeyCountResponse getPreKeyCount(@PathVariable String deviceId) {
        return deviceKeyService.getPreKeyCount(currentUserProvider.get(), deviceId);
    }
}
