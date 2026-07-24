package com.socialmedia.auth.controller;

import com.socialmedia.auth.dto.response.DeviceResponse;
import com.socialmedia.auth.dto.response.MessageResponse;
import com.socialmedia.auth.security.CurrentUserProvider;
import com.socialmedia.auth.service.DeviceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth/devices")
@Tag(name = "Device Management", description = "List and revoke devices linked to the account")
public class DeviceController {

    private final DeviceService deviceService;
    private final CurrentUserProvider currentUserProvider;

    public DeviceController(DeviceService deviceService, CurrentUserProvider currentUserProvider) {
        this.deviceService = deviceService;
        this.currentUserProvider = currentUserProvider;
    }

    @GetMapping
    @Operation(summary = "List devices linked to the current account")
    public List<DeviceResponse> list() {
        String currentDeviceId = currentUserProvider.principal().getDeviceId();
        return deviceService.listForUser(currentUserProvider.get(), currentDeviceId);
    }

    @DeleteMapping("/{deviceId}")
    @Operation(summary = "Revoke a device - ends its sessions and refresh tokens immediately")
    public MessageResponse revoke(@PathVariable String deviceId) {
        deviceService.revoke(currentUserProvider.get(), deviceId);
        return new MessageResponse("Device revoked");
    }
}
