package com.socialmedia.auth.service;

import com.socialmedia.auth.domain.User;
import com.socialmedia.auth.dto.response.LoginHistoryResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface LoginHistoryService {

    void record(User user, String ipAddress, String userAgent, boolean success, String failureReason);

    Page<LoginHistoryResponse> list(User user, Pageable pageable);
}
