package com.socialmedia.search.service.impl;

import com.socialmedia.search.config.SearchIndexNames;
import com.socialmedia.search.domain.ChatDocument;
import com.socialmedia.search.domain.MediaDocument;
import com.socialmedia.search.domain.UserDocument;
import com.socialmedia.search.dto.response.SearchHit;
import com.socialmedia.search.exception.SearchUnavailableException;
import com.socialmedia.search.service.SearchService;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch._types.query_dsl.Query;
import org.opensearch.client.opensearch.core.SearchResponse;
import org.opensearch.client.opensearch.core.search.Highlight;
import org.opensearch.client.opensearch.core.search.HighlightField;
import org.opensearch.client.opensearch.core.search.Hit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class SearchServiceImpl implements SearchService {

    private static final Logger log = LoggerFactory.getLogger(SearchServiceImpl.class);

    private final OpenSearchClient client;

    public SearchServiceImpl(OpenSearchClient client) {
        this.client = client;
    }

    @Override
    public void indexUser(UserDocument document) {
        index(SearchIndexNames.USERS, document.userId().toString(), document);
    }

    @Override
    public void deleteUser(UUID userId) {
        delete(SearchIndexNames.USERS, userId.toString());
    }

    @Override
    public void indexChat(ChatDocument document) {
        index(SearchIndexNames.CHATS, document.chatId().toString(), document);
    }

    @Override
    public void updateChatMeta(UUID chatId, String name, String description) {
        Map<String, Object> partial = new java.util.LinkedHashMap<>();
        if (name != null) {
            partial.put("name", name);
        }
        if (description != null) {
            partial.put("description", description);
        }
        if (partial.isEmpty()) {
            return;
        }
        try {
            client.update(u -> u.index(SearchIndexNames.CHATS).id(chatId.toString()).doc(partial), ChatDocument.class);
        } catch (IOException e) {
            log.warn("Failed to update chat {} in the search index: {}", chatId, e.getMessage());
        }
    }

    @Override
    public void deleteChat(UUID chatId) {
        delete(SearchIndexNames.CHATS, chatId.toString());
    }

    @Override
    public void indexMedia(MediaDocument document) {
        index(SearchIndexNames.MEDIA, document.mediaId().toString(), document);
    }

    @Override
    public void deleteMedia(UUID mediaId) {
        delete(SearchIndexNames.MEDIA, mediaId.toString());
    }

    @Override
    public List<SearchHit<UserDocument>> searchUsers(String query, int limit) {
        Query esQuery = Query.of(q -> q.multiMatch(m -> m
                .query(query)
                .fields(List.of("username^3", "displayName^2", "bio"))));
        return search(SearchIndexNames.USERS, esQuery, limit, UserDocument.class,
                highlightOf("username", "displayName", "bio"));
    }

    @Override
    public List<SearchHit<UserDocument>> autocompleteUsers(String query, int limit) {
        Query esQuery = Query.of(q -> q.matchBoolPrefix(m -> m.field("username").query(query)));
        return search(SearchIndexNames.USERS, esQuery, limit, UserDocument.class, null);
    }

    @Override
    public List<SearchHit<ChatDocument>> searchChannels(String query, int limit) {
        Query esQuery = Query.of(q -> q.multiMatch(m -> m
                .query(query)
                .fields(List.of("name^3", "description"))));
        return search(SearchIndexNames.CHATS, esQuery, limit, ChatDocument.class, highlightOf("name", "description"));
    }

    @Override
    public List<SearchHit<MediaDocument>> searchMedia(String query, int limit) {
        Query esQuery = Query.of(q -> q.match(m -> m.field("originalFilename").query(v -> v.stringValue(query))));
        return search(SearchIndexNames.MEDIA, esQuery, limit, MediaDocument.class, highlightOf("originalFilename"));
    }

    private Highlight highlightOf(String... fields) {
        Map<String, HighlightField> highlightFields = new java.util.LinkedHashMap<>();
        for (String field : fields) {
            highlightFields.put(field, HighlightField.of(f -> f));
        }
        return Highlight.of(h -> h.fields(highlightFields));
    }

    private <T> void index(String indexName, String id, T document) {
        try {
            client.index(i -> i.index(indexName).id(id).document(document));
        } catch (IOException e) {
            log.warn("Failed to index document {} in {}: {}", id, indexName, e.getMessage());
            throw new SearchUnavailableException(e);
        }
    }

    private void delete(String indexName, String id) {
        try {
            client.delete(d -> d.index(indexName).id(id));
        } catch (IOException e) {
            log.warn("Failed to delete document {} from {}: {}", id, indexName, e.getMessage());
            throw new SearchUnavailableException(e);
        }
    }

    private <T> List<SearchHit<T>> search(String indexName, Query query, int limit, Class<T> type, Highlight highlight) {
        try {
            SearchResponse<T> response = client.search(s -> {
                s.index(indexName).size(limit).query(query);
                if (highlight != null) {
                    s.highlight(highlight);
                }
                return s;
            }, type);

            return response.hits().hits().stream()
                    .map(this::toSearchHit)
                    .toList();
        } catch (IOException e) {
            log.warn("Search against index {} failed: {}", indexName, e.getMessage());
            throw new SearchUnavailableException(e);
        }
    }

    private <T> SearchHit<T> toSearchHit(Hit<T> hit) {
        double score = hit.score() == null ? 0.0 : hit.score();
        return new SearchHit<>(hit.source(), score, hit.highlight());
    }
}
