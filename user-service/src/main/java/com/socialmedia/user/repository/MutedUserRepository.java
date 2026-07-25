package com.socialmedia.user.repository;

import com.socialmedia.user.domain.MutedUser;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MutedUserRepository extends JpaRepository<MutedUser, UUID> {

    List<MutedUser> findByMuterId(UUID muterId);

    Optional<MutedUser> findByMuterIdAndMutedId(UUID muterId, UUID mutedId);

    void deleteByMuterIdAndMutedId(UUID muterId, UUID mutedId);
}
