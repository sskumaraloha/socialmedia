package com.socialmedia.user.repository;

import com.socialmedia.user.domain.BlockedUser;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BlockedUserRepository extends JpaRepository<BlockedUser, UUID> {

    List<BlockedUser> findByBlockerId(UUID blockerId);

    boolean existsByBlockerIdAndBlockedId(UUID blockerId, UUID blockedId);

    /** Blocking is asymmetric but its effect (e.g. "can't message") is usually checked both ways. */
    boolean existsByBlockerIdAndBlockedIdOrBlockerIdAndBlockedId(UUID blockerId1, UUID blockedId1, UUID blockerId2,
            UUID blockedId2);

    void deleteByBlockerIdAndBlockedId(UUID blockerId, UUID blockedId);
}
