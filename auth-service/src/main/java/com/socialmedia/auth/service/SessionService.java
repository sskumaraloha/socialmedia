package com.socialmedia.auth.service;

import com.socialmedia.auth.domain.User;
import com.socialmedia.auth.dto.response.SessionResponse;
import java.util.UUID;
import java.util.List;

public interface SessionService {

    List<SessionResponse> listForUser(User user, String currentJti);

    void revoke(User user, UUID sessionId);

    void revokeAllExceptCurrent(User user, String currentJti);
}
