package com.socialmedia.auth.repository;

import com.socialmedia.auth.domain.Session;
import com.socialmedia.auth.domain.User;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SessionRepository extends JpaRepository<Session, UUID> {

    Optional<Session> findByJti(String jti);

    List<Session> findByUserAndRevokedFalseOrderByLastSeenAtDesc(User user);

    List<Session> findByUserAndDeviceIdAndRevokedFalse(User user, String deviceId);

    @Modifying
    @Query("update Session s set s.revoked = true where s.user = :user")
    void revokeAllForUser(@Param("user") User user);

    @Modifying
    @Query("update Session s set s.revoked = true where s.id = :id and s.user = :user")
    int revokeByIdForUser(@Param("id") UUID id, @Param("user") User user);
}
