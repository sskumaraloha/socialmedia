package com.socialmedia.chat.repository;

import com.socialmedia.chat.domain.ChatMember;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ChatMemberRepository extends JpaRepository<ChatMember, UUID> {

    Optional<ChatMember> findByChatIdAndUserId(UUID chatId, UUID userId);

    List<ChatMember> findAllByChatId(UUID chatId);

    List<ChatMember> findAllByUserId(UUID userId);

    long countByChatId(UUID chatId);

    boolean existsByChatIdAndUserId(UUID chatId, UUID userId);

    void deleteByChatId(UUID chatId);

    @Modifying
    @Query("update ChatMember m set m.unreadCount = m.unreadCount + 1 where m.chatId = :chatId and m.userId <> :senderId")
    void incrementUnreadForOthers(@Param("chatId") UUID chatId, @Param("senderId") UUID senderId);
}
