package com.socialmedia.search.service;

import com.socialmedia.search.domain.ChatDocument;
import com.socialmedia.search.domain.MediaDocument;
import com.socialmedia.search.domain.MessageDocument;
import com.socialmedia.search.domain.UserDocument;
import com.socialmedia.search.dto.response.SearchHit;
import java.util.List;
import java.util.UUID;

public interface SearchService {

    void indexUser(UserDocument document);

    void deleteUser(UUID userId);

    void indexChat(ChatDocument document);

    void updateChatMeta(UUID chatId, String name, String description);

    void deleteChat(UUID chatId);

    void indexMessage(MessageDocument document);

    void deleteMessage(UUID messageId);

    void indexMedia(MediaDocument document);

    void deleteMedia(UUID mediaId);

    List<SearchHit<UserDocument>> searchUsers(String query, int limit);

    List<SearchHit<UserDocument>> autocompleteUsers(String query, int limit);

    List<SearchHit<ChatDocument>> searchChannels(String query, int limit);

    List<SearchHit<MessageDocument>> searchMessages(String query, UUID chatId, int limit);

    List<SearchHit<MediaDocument>> searchMedia(String query, int limit);
}
