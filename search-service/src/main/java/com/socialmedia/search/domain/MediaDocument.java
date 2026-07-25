package com.socialmedia.search.domain;

import java.util.UUID;

public record MediaDocument(UUID mediaId, UUID ownerId, String originalFilename, String kind) {
}
