package com.socialmedia.auth.repository;

import com.socialmedia.auth.domain.DeviceIdentityKey;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeviceIdentityKeyRepository extends JpaRepository<DeviceIdentityKey, UUID> {

    Optional<DeviceIdentityKey> findByDeviceId(String deviceId);
}
