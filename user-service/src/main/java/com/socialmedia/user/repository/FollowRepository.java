package com.socialmedia.user.repository;

import com.socialmedia.user.domain.Follow;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FollowRepository extends JpaRepository<Follow, UUID> {

    Optional<Follow> findByFollowerIdAndFollowingId(UUID followerId, UUID followingId);

    boolean existsByFollowerIdAndFollowingId(UUID followerId, UUID followingId);

    Page<Follow> findByFollowingId(UUID followingId, Pageable pageable);

    Page<Follow> findByFollowerId(UUID followerId, Pageable pageable);

    long countByFollowingId(UUID followingId);

    long countByFollowerId(UUID followerId);

    void deleteByFollowerIdAndFollowingId(UUID followerId, UUID followingId);
}
