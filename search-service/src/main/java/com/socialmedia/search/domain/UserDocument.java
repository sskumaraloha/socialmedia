package com.socialmedia.search.domain;

import java.util.UUID;

public record UserDocument(UUID userId, String username, String displayName, String bio) {
}
