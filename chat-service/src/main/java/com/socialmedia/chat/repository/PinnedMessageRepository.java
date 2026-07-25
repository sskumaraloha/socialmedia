package com.socialmedia.chat.repository;

import com.socialmedia.chat.domain.PinnedMessage;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PinnedMessageRepository extends JpaRepository<PinnedMessage, UUID> {

    List<PinnedMessage> findAllByChatIdOrderByCreatedAtDesc(UUID chatId);

    Optional<PinnedMessage> findByChatIdAndMessageId(UUID chatId, UUID messageId);

    void deleteByChatIdAndMessageId(UUID chatId, UUID messageId);

    void deleteByChatId(UUID chatId);

    long countByChatId(UUID chatId);
}
