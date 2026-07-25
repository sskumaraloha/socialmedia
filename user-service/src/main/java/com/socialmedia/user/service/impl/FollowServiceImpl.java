package com.socialmedia.user.service.impl;

import com.socialmedia.common.exception.ResourceNotFoundException;
import com.socialmedia.user.domain.Follow;
import com.socialmedia.user.dto.response.UserSummaryResponse;
import com.socialmedia.user.event.UserEventPublisher;
import com.socialmedia.user.exception.CannotActOnSelfException;
import com.socialmedia.user.mapper.UserProfileMapper;
import com.socialmedia.user.repository.FollowRepository;
import com.socialmedia.user.repository.UserProfileRepository;
import com.socialmedia.user.service.FollowService;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FollowServiceImpl implements FollowService {

    private final FollowRepository followRepository;
    private final UserProfileRepository userProfileRepository;
    private final UserProfileMapper mapper;
    private final UserEventPublisher eventPublisher;

    public FollowServiceImpl(FollowRepository followRepository, UserProfileRepository userProfileRepository,
            UserProfileMapper mapper, UserEventPublisher eventPublisher) {
        this.followRepository = followRepository;
        this.userProfileRepository = userProfileRepository;
        this.mapper = mapper;
        this.eventPublisher = eventPublisher;
    }

    @Override
    @Transactional
    public void follow(UUID followerId, UUID followingId) {
        if (followerId.equals(followingId)) {
            throw new CannotActOnSelfException("follow");
        }
        if (!userProfileRepository.existsById(followingId)) {
            throw new ResourceNotFoundException("UserProfile", followingId);
        }
        if (followRepository.existsByFollowerIdAndFollowingId(followerId, followingId)) {
            return; // idempotent
        }
        followRepository.save(new Follow(followerId, followingId));
        eventPublisher.publishFollowed(followerId, followingId);
    }

    @Override
    @Transactional
    public void unfollow(UUID followerId, UUID followingId) {
        followRepository.deleteByFollowerIdAndFollowingId(followerId, followingId);
        eventPublisher.publishUnfollowed(followerId, followingId);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserSummaryResponse> listFollowers(UUID userId, Pageable pageable) {
        return followRepository.findByFollowingId(userId, pageable)
                .map(f -> userProfileRepository.findById(f.getFollowerId()).orElse(null))
                .map(profile -> profile == null ? null : mapper.toSummary(profile));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserSummaryResponse> listFollowing(UUID userId, Pageable pageable) {
        return followRepository.findByFollowerId(userId, pageable)
                .map(f -> userProfileRepository.findById(f.getFollowingId()).orElse(null))
                .map(profile -> profile == null ? null : mapper.toSummary(profile));
    }
}
