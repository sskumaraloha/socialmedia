package com.socialmedia.notification.repository;

import com.socialmedia.notification.domain.DeviceToken;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeviceTokenRepository extends JpaRepository<DeviceToken, UUID> {

    List<DeviceToken> findAllByUserId(UUID userId);

    Optional<DeviceToken> findByUserIdAndToken(UUID userId, String token);

    void deleteByUserIdAndToken(UUID userId, String token);
}
