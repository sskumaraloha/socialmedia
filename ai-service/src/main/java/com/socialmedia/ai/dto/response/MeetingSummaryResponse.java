package com.socialmedia.ai.dto.response;

import java.util.List;

public record MeetingSummaryResponse(String summary, List<String> keyPoints, List<String> actionItems) {
}
