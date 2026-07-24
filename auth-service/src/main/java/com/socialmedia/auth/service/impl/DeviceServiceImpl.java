package com.socialmedia.auth.service.impl;

import com.socialmedia.auth.domain.Device;
import com.socialmedia.auth.domain.DeviceType;
import com.socialmedia.auth.domain.Session;
import com.socialmedia.auth.domain.User;
import com.socialmedia.auth.dto.response.DeviceResponse;
import com.socialmedia.auth.event.AuthEventPublisher;
import com.socialmedia.auth.repository.DeviceRepository;
import com.socialmedia.auth.repository.RefreshTokenRepository;
import com.socialmedia.auth.repository.SessionRepository;
import com.socialmedia.auth.service.DeviceService;
import com.socialmedia.auth.service.SessionCacheService;
import com.socialmedia.common.exception.ResourceNotFoundException;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DeviceServiceImpl implements DeviceService {

    private final DeviceRepository deviceRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final SessionRepository sessionRepository;
    private final SessionCacheService sessionCacheService;
    private final AuthEventPublisher eventPublisher;

    public DeviceServiceImpl(DeviceRepository deviceRepository, RefreshTokenRepository refreshTokenRepository,
            SessionRepository sessionRepository, SessionCacheService sessionCacheService,
            AuthEventPublisher eventPublisher) {
        this.deviceRepository = deviceRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.sessionRepository = sessionRepository;
        this.sessionCacheService = sessionCacheService;
        this.eventPublisher = eventPublisher;
    }

    @Override
    @Transactional
    public Device upsert(User user, String deviceId, String deviceName, DeviceType deviceType) {
        Device device = deviceRepository.findByDeviceId(deviceId)
                .orElseGet(() -> new Device(user, deviceId, deviceName, deviceType == null ? DeviceType.UNKNOWN : deviceType));
        device.touch();
        if (deviceName != null) {
            device.setDeviceName(deviceName);
        }
        return deviceRepository.save(device);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DeviceResponse> listForUser(User user, String currentDeviceId) {
        return deviceRepository.findByUserAndRevokedFalseOrderByLastActiveAtDesc(user).stream()
                .map(d -> new DeviceResponse(d.getId(), d.getDeviceId(), d.getDeviceName(), d.getDeviceType(),
                        d.getLastActiveAt(), d.isTrusted(), d.getDeviceId().equals(currentDeviceId)))
                .toList();
    }

    @Override
    @Transactional
    public void revoke(User user, String deviceId) {
        Device device = deviceRepository.findByUserAndDeviceId(user, deviceId)
                .orElseThrow(() -> new ResourceNotFoundException("Device", deviceId));
        device.setRevoked(true);

        refreshTokenRepository.revokeAllForUserAndDevice(user, deviceId);

        for (Session session : sessionRepository.findByUserAndDeviceIdAndRevokedFalse(user, deviceId)) {
            session.setRevoked(true);
            sessionCacheService.evict(session.getJti());
        }

        eventPublisher.publishSessionRevoked(user.getId(), null, deviceId, "DEVICE_REVOKED");
    }
}
