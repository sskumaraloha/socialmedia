package com.socialmedia.chat.repository;

import com.socialmedia.chat.domain.Chat;
import com.socialmedia.chat.domain.ChatType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ChatRepository extends JpaRepository<Chat, UUID> {

    @Query("""
            select c from Chat c
            join ChatMember m on m.chatId = c.id
            where m.userId = :userId and c.type = :type
            """)
    List<Chat> findAllByMemberAndType(@Param("userId") UUID userId, @Param("type") ChatType type);

    @Query("""
            select distinct c.id from Chat c
            join ChatMember m1 on m1.chatId = c.id
            join ChatMember m2 on m2.chatId = c.id
            where c.type = com.socialmedia.chat.domain.ChatType.PRIVATE
              and m1.userId = :userA and m2.userId = :userB
            """)
    Optional<UUID> findExistingPrivateChatId(@Param("userA") UUID userA, @Param("userB") UUID userB);
}
