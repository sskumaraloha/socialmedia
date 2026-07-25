package com.socialmedia.message.repository;

import com.socialmedia.message.domain.Message;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface MessageRepository extends MongoRepository<Message, UUID> {

    List<Message> findAllByChatIdAndCreatedAtBeforeOrderByCreatedAtDesc(UUID chatId, Instant before, org.springframework.data.domain.Pageable pageable);

    List<Message> findAllByChatIdOrderByCreatedAtDesc(UUID chatId, org.springframework.data.domain.Pageable pageable);

    List<Message> findAllByIdIn(List<UUID> ids);

    List<Message> findAllBySentFalseAndScheduledAtLessThanEqual(Instant now);

    List<Message> findAllBySelfDestructedFalseAndSelfDestructAtLessThanEqual(Instant now);
}
