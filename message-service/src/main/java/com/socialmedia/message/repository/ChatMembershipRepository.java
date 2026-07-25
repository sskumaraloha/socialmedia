package com.socialmedia.message.repository;

import com.socialmedia.message.domain.ChatMembership;
import java.util.List;
import java.util.UUID;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ChatMembershipRepository extends MongoRepository<ChatMembership, UUID> {

    List<ChatMembership> findAllByChatId(UUID chatId);

    void deleteByChatIdAndUserId(UUID chatId, UUID userId);

    void deleteByChatId(UUID chatId);

    boolean existsByChatIdAndUserId(UUID chatId, UUID userId);
}
