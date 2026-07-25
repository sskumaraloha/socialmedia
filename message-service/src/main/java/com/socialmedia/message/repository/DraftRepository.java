package com.socialmedia.message.repository;

import com.socialmedia.message.domain.Draft;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface DraftRepository extends MongoRepository<Draft, UUID> {

    Optional<Draft> findByChatIdAndUserId(UUID chatId, UUID userId);

    void deleteByChatIdAndUserId(UUID chatId, UUID userId);
}
