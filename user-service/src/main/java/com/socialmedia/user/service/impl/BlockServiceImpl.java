package com.socialmedia.user.service.impl;

import com.socialmedia.common.exception.ResourceNotFoundException;
import com.socialmedia.user.domain.BlockedUser;
import com.socialmedia.user.dto.response.UserSummaryResponse;
import com.socialmedia.user.event.UserEventPublisher;
import com.socialmedia.user.exception.CannotActOnSelfException;
import com.socialmedia.user.mapper.UserProfileMapper;
import com.socialmedia.user.repository.BlockedUserRepository;
import com.socialmedia.user.repository.FollowRepository;
import com.socialmedia.user.repository.UserProfileRepository;
import com.socialmedia.user.service.BlockService;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BlockServiceImpl implements BlockService {

    private final BlockedUserRepository blockedUserRepository;
    private final UserProfileRepository userProfileRepository;
    private final FollowRepository followRepository;
    private final UserProfileMapper mapper;
    private final UserEventPublisher eventPublisher;

    public BlockServiceImpl(BlockedUserRepository blockedUserRepository, UserProfileRepository userProfileRepository,
            FollowRepository followRepository, UserProfileMapper mapper, UserEventPublisher eventPublisher) {
        this.blockedUserRepository = blockedUserRepository;
        this.userProfileRepository = userProfileRepository;
        this.followRepository = followRepository;
        this.mapper = mapper;
        this.eventPublisher = eventPublisher;
    }

    @Override
    @Transactional
    public void block(UUID blockerId, UUID blockedId) {
        if (blockerId.equals(blockedId)) {
            throw new CannotActOnSelfException("block");
        }
        if (!userProfileRepository.existsById(blockedId)) {
            throw new ResourceNotFoundException("UserProfile", blockedId);
        }
        if (!blockedUserRepository.existsByBlockerIdAndBlockedId(blockerId, blockedId)) {
            blockedUserRepository.save(new BlockedUser(blockerId, blockedId));
        }
        // Blocking someone severs any existing follow relationship in both directions.
        followRepository.deleteByFollowerIdAndFollowingId(blockerId, blockedId);
        followRepository.deleteByFollowerIdAndFollowingId(blockedId, blockerId);
        eventPublisher.publishBlocked(blockerId, blockedId);
    }

    @Override
    @Transactional
    public void unblock(UUID blockerId, UUID blockedId) {
        blockedUserRepository.deleteByBlockerIdAndBlockedId(blockerId, blockedId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserSummaryResponse> listBlocked(UUID blockerId) {
        List<UUID> blockedIds = blockedUserRepository.findByBlockerId(blockerId).stream()
                .map(BlockedUser::getBlockedId)
                .collect(Collectors.toList());
        return userProfileRepository.findByIdIn(blockedIds).stream()
                .map(mapper::toSummary)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isBlockedEitherWay(UUID userA, UUID userB) {
        return blockedUserRepository.existsByBlockerIdAndBlockedIdOrBlockerIdAndBlockedId(userA, userB, userB, userA);
    }
}
