package com.socialmedia.auth.service.impl;

import com.socialmedia.auth.domain.Session;
import com.socialmedia.auth.domain.User;
import com.socialmedia.auth.dto.response.SessionResponse;
import com.socialmedia.auth.event.AuthEventPublisher;
import com.socialmedia.auth.repository.SessionRepository;
import com.socialmedia.auth.service.SessionCacheService;
import com.socialmedia.auth.service.SessionService;
import com.socialmedia.common.exception.ResourceNotFoundException;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SessionServiceImpl implements SessionService {

    private final SessionRepository sessionRepository;
    private final SessionCacheService sessionCacheService;
    private final AuthEventPublisher eventPublisher;

    public SessionServiceImpl(SessionRepository sessionRepository, SessionCacheService sessionCacheService,
            AuthEventPublisher eventPublisher) {
        this.sessionRepository = sessionRepository;
        this.sessionCacheService = sessionCacheService;
        this.eventPublisher = eventPublisher;
    }

    @Override
    @Transactional(readOnly = true)
    public List<SessionResponse> listForUser(User user, String currentJti) {
        return sessionRepository.findByUserAndRevokedFalseOrderByLastSeenAtDesc(user).stream()
                .map(s -> new SessionResponse(s.getId(), s.getDeviceId(), s.getIpAddress(), s.getUserAgent(),
                        s.getIssuedAt(), s.getLastSeenAt(), s.getJti().equals(currentJti)))
                .toList();
    }

    @Override
    @Transactional
    public void revoke(User user, UUID sessionId) {
        Session session = sessionRepository.findById(sessionId)
                .filter(s -> s.getUser().getId().equals(user.getId()))
                .orElseThrow(() -> new ResourceNotFoundException("Session", sessionId));
        session.setRevoked(true);
        sessionCacheService.evict(session.getJti());
        eventPublisher.publishSessionRevoked(user.getId(), session.getId(), session.getDeviceId(), "USER_REVOKED");
    }

    @Override
    @Transactional
    public void revokeAllExceptCurrent(User user, String currentJti) {
        for (Session session : sessionRepository.findByUserAndRevokedFalseOrderByLastSeenAtDesc(user)) {
            if (session.getJti().equals(currentJti)) {
                continue;
            }
            session.setRevoked(true);
            sessionCacheService.evict(session.getJti());
        }
        eventPublisher.publishSessionRevoked(user.getId(), null, null, "LOGOUT_ALL_OTHER_DEVICES");
    }
}
