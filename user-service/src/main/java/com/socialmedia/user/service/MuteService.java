package com.socialmedia.user.service;

import com.socialmedia.user.dto.request.MuteUserRequest;
import com.socialmedia.user.dto.response.UserSummaryResponse;
import java.util.List;
import java.util.UUID;

public interface MuteService {

    void mute(UUID muterId, UUID mutedId, MuteUserRequest request);

    void unmute(UUID muterId, UUID mutedId);

    List<UserSummaryResponse> listMuted(UUID muterId);

    boolean isMuted(UUID muterId, UUID mutedId);
}
