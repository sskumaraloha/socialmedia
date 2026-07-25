package com.socialmedia.search.controller;

import com.socialmedia.search.domain.ChatDocument;
import com.socialmedia.search.domain.MediaDocument;
import com.socialmedia.search.domain.MessageDocument;
import com.socialmedia.search.domain.UserDocument;
import com.socialmedia.search.dto.response.SearchHit;
import com.socialmedia.search.service.SearchService;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/search")
public class SearchController {

    private static final int MAX_LIMIT = 100;
    private static final int DEFAULT_LIMIT = 20;

    private final SearchService searchService;

    public SearchController(SearchService searchService) {
        this.searchService = searchService;
    }

    @GetMapping("/users")
    public List<SearchHit<UserDocument>> searchUsers(@RequestParam String q, @RequestParam(defaultValue = "20") int limit) {
        return searchService.searchUsers(q, clamp(limit));
    }

    @GetMapping("/users/autocomplete")
    public List<SearchHit<UserDocument>> autocompleteUsers(@RequestParam String q, @RequestParam(defaultValue = "10") int limit) {
        return searchService.autocompleteUsers(q, clamp(limit));
    }

    @GetMapping("/channels")
    public List<SearchHit<ChatDocument>> searchChannels(@RequestParam String q, @RequestParam(defaultValue = "20") int limit) {
        return searchService.searchChannels(q, clamp(limit));
    }

    @GetMapping("/messages")
    public List<SearchHit<MessageDocument>> searchMessages(@RequestParam String q,
            @RequestParam(required = false) UUID chatId, @RequestParam(defaultValue = "20") int limit) {
        return searchService.searchMessages(q, chatId, clamp(limit));
    }

    @GetMapping("/media")
    public List<SearchHit<MediaDocument>> searchMedia(@RequestParam String q, @RequestParam(defaultValue = "20") int limit) {
        return searchService.searchMedia(q, clamp(limit));
    }

    private int clamp(int limit) {
        return Math.max(1, Math.min(limit, MAX_LIMIT));
    }
}
