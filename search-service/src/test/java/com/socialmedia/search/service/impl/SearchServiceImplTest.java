package com.socialmedia.search.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.socialmedia.search.config.SearchIndexNames;
import com.socialmedia.search.domain.UserDocument;
import com.socialmedia.search.dto.response.SearchHit;
import com.socialmedia.search.exception.SearchUnavailableException;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch.core.SearchResponse;
import org.opensearch.client.opensearch.core.search.Hit;
import org.opensearch.client.opensearch.core.search.HitsMetadata;

@ExtendWith(MockitoExtension.class)
class SearchServiceImplTest {

    @Mock private OpenSearchClient client;

    private SearchServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new SearchServiceImpl(client);
    }

    @Test
    void indexUserSendsAnIndexRequestForTheUsersIndex() throws IOException {
        UserDocument document = new UserDocument(UUID.randomUUID(), "alice", "Alice", "hi there");

        service.indexUser(document);

        verify(client).index(any(Function.class));
    }

    @Test
    void indexUserWrapsAnIoExceptionAsSearchUnavailable() throws IOException {
        when(client.index(any(Function.class))).thenThrow(new IOException("connection refused"));
        UserDocument document = new UserDocument(UUID.randomUUID(), "alice", "Alice", "hi there");

        assertThatThrownBy(() -> service.indexUser(document)).isInstanceOf(SearchUnavailableException.class);
    }

    @Test
    void deleteUserSendsADeleteRequest() throws IOException {
        UUID userId = UUID.randomUUID();

        service.deleteUser(userId);

        verify(client).delete(any(Function.class));
    }

    @Test
    void searchUsersMapsHitsToSearchResults() throws IOException {
        UUID userId = UUID.randomUUID();
        UserDocument document = new UserDocument(userId, "alice", "Alice", "hi there");
        Hit<UserDocument> hit = Hit.of(h -> h.index(SearchIndexNames.USERS).id(userId.toString()).source(document).score(1.5));
        HitsMetadata<UserDocument> hitsMetadata = HitsMetadata.of(hm -> hm.hits(List.of(hit)));
        SearchResponse<UserDocument> response = SearchResponse.searchResponseOf(r -> r
                .took(1)
                .timedOut(false)
                .shards(sh -> sh.total(1).successful(1).failed(0))
                .hits(hitsMetadata));
        when(client.search(any(Function.class), org.mockito.ArgumentMatchers.eq(UserDocument.class))).thenReturn(response);

        List<SearchHit<UserDocument>> results = service.searchUsers("alice", 10);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).document().username()).isEqualTo("alice");
        assertThat(results.get(0).score()).isEqualTo(1.5);
    }

    @Test
    void searchUsersWrapsAnIoExceptionAsSearchUnavailable() throws IOException {
        when(client.search(any(Function.class), org.mockito.ArgumentMatchers.eq(UserDocument.class)))
                .thenThrow(new IOException("timeout"));

        assertThatThrownBy(() -> service.searchUsers("alice", 10)).isInstanceOf(SearchUnavailableException.class);
    }
}
