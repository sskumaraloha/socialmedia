package com.socialmedia.auth.dto.response;

public record PreKeyCountResponse(String deviceId, long remainingOneTimePreKeys) {
}
