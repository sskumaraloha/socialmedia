package com.socialmedia.auth.repository;

import com.socialmedia.auth.domain.OneTimePreKey;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OneTimePreKeyRepository extends JpaRepository<OneTimePreKey, UUID> {

    Optional<OneTimePreKey> findFirstByDeviceIdAndConsumedFalseOrderByCreatedAtAsc(String deviceId);

    long countByDeviceIdAndConsumedFalse(String deviceId);

    Optional<OneTimePreKey> findByDeviceIdAndKeyId(String deviceId, int keyId);
}
