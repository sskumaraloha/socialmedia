package com.socialmedia.ai.client;

import java.util.List;

public record ChatCompletionRequest(String model, List<ChatMessage> messages, double temperature) {
}
