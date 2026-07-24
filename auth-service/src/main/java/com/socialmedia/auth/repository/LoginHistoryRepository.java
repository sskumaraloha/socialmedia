package com.socialmedia.auth.repository;

import com.socialmedia.auth.domain.LoginHistory;
import com.socialmedia.auth.domain.User;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LoginHistoryRepository extends JpaRepository<LoginHistory, UUID> {

    Page<LoginHistory> findByUserOrderByCreatedAtDesc(User user, Pageable pageable);
}
