package com.socialmedia.ai.client;

import java.util.List;

public record ChatCompletionResponse(List<Choice> choices) {

    public record Choice(ChatMessage message) {
    }

    public String firstContent() {
        if (choices == null || choices.isEmpty()) {
            return null;
        }
        return choices.get(0).message().content();
    }
}
