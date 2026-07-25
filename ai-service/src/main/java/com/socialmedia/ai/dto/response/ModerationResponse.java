package com.socialmedia.ai.dto.response;

import java.util.List;

public record ModerationResponse(boolean flagged, List<String> categories, String reason) {
}
