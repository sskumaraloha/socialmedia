package com.socialmedia.auth.repository;

import com.socialmedia.auth.domain.Device;
import com.socialmedia.auth.domain.User;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeviceRepository extends JpaRepository<Device, UUID> {

    Optional<Device> findByDeviceId(String deviceId);

    List<Device> findByUserAndRevokedFalseOrderByLastActiveAtDesc(User user);

    Optional<Device> findByUserAndDeviceId(User user, String deviceId);
}
