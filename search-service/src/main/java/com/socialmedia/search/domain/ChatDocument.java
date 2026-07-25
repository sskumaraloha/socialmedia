package com.socialmedia.search.domain;

import java.util.UUID;

/** Only GROUP/CHANNEL chats are indexed - a private chat has no name to search by. */
public record ChatDocument(UUID chatId, String type, String name, String description) {
}
