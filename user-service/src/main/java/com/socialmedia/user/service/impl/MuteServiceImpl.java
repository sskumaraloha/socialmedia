package com.socialmedia.user.service.impl;

import com.socialmedia.common.exception.ResourceNotFoundException;
import com.socialmedia.user.domain.MutedUser;
import com.socialmedia.user.dto.request.MuteUserRequest;
import com.socialmedia.user.dto.response.UserSummaryResponse;
import com.socialmedia.user.exception.CannotActOnSelfException;
import com.socialmedia.user.mapper.UserProfileMapper;
import com.socialmedia.user.repository.MutedUserRepository;
import com.socialmedia.user.repository.UserProfileRepository;
import com.socialmedia.user.service.MuteService;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MuteServiceImpl implements MuteService {

    private final MutedUserRepository mutedUserRepository;
    private final UserProfileRepository userProfileRepository;
    private final UserProfileMapper mapper;

    public MuteServiceImpl(MutedUserRepository mutedUserRepository, UserProfileRepository userProfileRepository,
            UserProfileMapper mapper) {
        this.mutedUserRepository = mutedUserRepository;
        this.userProfileRepository = userProfileRepository;
        this.mapper = mapper;
    }

    @Override
    @Transactional
    public void mute(UUID muterId, UUID mutedId, MuteUserRequest request) {
        if (muterId.equals(mutedId)) {
            throw new CannotActOnSelfException("mute");
        }
        if (!userProfileRepository.existsById(mutedId)) {
            throw new ResourceNotFoundException("UserProfile", mutedId);
        }
        mutedUserRepository.findByMuterIdAndMutedId(muterId, mutedId)
                .ifPresentOrElse(
                        existing -> { /* already muted - request.mutedUntil() update handled via unmute+mute for simplicity */ },
                        () -> mutedUserRepository.save(new MutedUser(muterId, mutedId, request.mutedUntil())));
    }

    @Override
    @Transactional
    public void unmute(UUID muterId, UUID mutedId) {
        mutedUserRepository.deleteByMuterIdAndMutedId(muterId, mutedId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserSummaryResponse> listMuted(UUID muterId) {
        List<UUID> mutedIds = mutedUserRepository.findByMuterId(muterId).stream()
                .filter(MutedUser::isActive)
                .map(MutedUser::getMutedId)
                .collect(Collectors.toList());
        return userProfileRepository.findByIdIn(mutedIds).stream()
                .map(mapper::toSummary)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isMuted(UUID muterId, UUID mutedId) {
        return mutedUserRepository.findByMuterIdAndMutedId(muterId, mutedId)
                .map(MutedUser::isActive)
                .orElse(false);
    }
}
