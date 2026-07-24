package com.socialmedia.auth.repository;

import com.socialmedia.auth.domain.AuthProvider;
import com.socialmedia.auth.domain.User;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    Optional<User> findByProviderAndProviderId(AuthProvider provider, String providerId);
}
