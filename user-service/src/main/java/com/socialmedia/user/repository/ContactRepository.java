package com.socialmedia.user.repository;

import com.socialmedia.user.domain.Contact;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContactRepository extends JpaRepository<Contact, UUID> {

    List<Contact> findByOwnerId(UUID ownerId);

    Optional<Contact> findByOwnerIdAndContactUserId(UUID ownerId, UUID contactUserId);

    boolean existsByOwnerIdAndContactUserId(UUID ownerId, UUID contactUserId);

    void deleteByOwnerIdAndContactUserId(UUID ownerId, UUID contactUserId);
}
