package com.socialmedia.auth.service.impl;

import com.socialmedia.auth.domain.LoginHistory;
import com.socialmedia.auth.domain.User;
import com.socialmedia.auth.dto.response.LoginHistoryResponse;
import com.socialmedia.auth.repository.LoginHistoryRepository;
import com.socialmedia.auth.service.LoginHistoryService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LoginHistoryServiceImpl implements LoginHistoryService {

    private final LoginHistoryRepository loginHistoryRepository;

    public LoginHistoryServiceImpl(LoginHistoryRepository loginHistoryRepository) {
        this.loginHistoryRepository = loginHistoryRepository;
    }

    @Override
    @Transactional
    public void record(User user, String ipAddress, String userAgent, boolean success, String failureReason) {
        loginHistoryRepository.save(new LoginHistory(user, ipAddress, userAgent, success, failureReason));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<LoginHistoryResponse> list(User user, Pageable pageable) {
        return loginHistoryRepository.findByUserOrderByCreatedAtDesc(user, pageable)
                .map(h -> new LoginHistoryResponse(h.getId(), h.getIpAddress(), h.getUserAgent(), h.isSuccess(),
                        h.getFailureReason(), h.getCreatedAt()));
    }
}
