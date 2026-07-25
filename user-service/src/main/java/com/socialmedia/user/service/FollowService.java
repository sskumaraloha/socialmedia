package com.socialmedia.user.service;

import com.socialmedia.user.dto.response.UserSummaryResponse;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface FollowService {

    void follow(UUID followerId, UUID followingId);

    void unfollow(UUID followerId, UUID followingId);

    Page<UserSummaryResponse> listFollowers(UUID userId, Pageable pageable);

    Page<UserSummaryResponse> listFollowing(UUID userId, Pageable pageable);
}
