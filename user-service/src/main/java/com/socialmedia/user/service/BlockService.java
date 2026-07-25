package com.socialmedia.user.service;

import com.socialmedia.user.dto.response.UserSummaryResponse;
import java.util.List;
import java.util.UUID;

public interface BlockService {

    void block(UUID blockerId, UUID blockedId);

    void unblock(UUID blockerId, UUID blockedId);

    List<UserSummaryResponse> listBlocked(UUID blockerId);

    boolean isBlockedEitherWay(UUID userA, UUID userB);
}
