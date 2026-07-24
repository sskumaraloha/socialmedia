package com.socialmedia.auth.mapper;

import com.socialmedia.auth.domain.User;
import com.socialmedia.auth.dto.response.UserSummaryResponse;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public UserSummaryResponse toSummary(User user) {
        return new UserSummaryResponse(
                user.getId(),
                user.getEmail(),
                user.isEmailVerified(),
                user.isTwoFactorEnabled(),
                user.getProvider(),
                user.getRoles()
        );
    }
}
