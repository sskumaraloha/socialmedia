package com.socialmedia.auth.repository;

import com.socialmedia.auth.domain.RefreshToken;
import com.socialmedia.auth.domain.User;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    List<RefreshToken> findByUserAndRevokedFalse(User user);

    @Modifying
    @Query("update RefreshToken r set r.revoked = true where r.user = :user")
    void revokeAllForUser(@Param("user") User user);

    @Modifying
    @Query("update RefreshToken r set r.revoked = true where r.deviceId = :deviceId and r.user = :user")
    void revokeAllForUserAndDevice(@Param("user") User user, @Param("deviceId") String deviceId);

    @Modifying
    @Query("delete from RefreshToken r where r.expiresAt < :cutoff")
    int deleteExpiredBefore(@Param("cutoff") Instant cutoff);
}
