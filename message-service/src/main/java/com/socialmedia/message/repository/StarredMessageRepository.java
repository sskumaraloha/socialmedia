package com.socialmedia.message.repository;

import com.socialmedia.message.domain.StarredMessage;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface StarredMessageRepository extends MongoRepository<StarredMessage, UUID> {

    Optional<StarredMessage> findByUserIdAndMessageId(UUID userId, UUID messageId);

    List<StarredMessage> findAllByUserIdOrderByStarredAtDesc(UUID userId);

    void deleteByUserIdAndMessageId(UUID userId, UUID messageId);
}
