package com.socialmedia.user.repository;

import com.socialmedia.user.domain.UserProfile;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserProfileRepository extends JpaRepository<UserProfile, UUID> {

    Optional<UserProfile> findByUsernameIgnoreCase(String username);

    boolean existsByUsernameIgnoreCase(String username);

    @Query("select p from UserProfile p where lower(p.username) like lower(concat('%', :query, '%')) "
            + "or lower(p.displayName) like lower(concat('%', :query, '%'))")
    Page<UserProfile> search(@Param("query") String query, Pageable pageable);

    List<UserProfile> findByIdIn(List<UUID> ids);
}
