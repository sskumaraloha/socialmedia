package com.socialmedia.ai.client;

import com.socialmedia.ai.config.OpenAiProperties;
import com.socialmedia.ai.exception.AiServiceUnavailableException;
import java.time.Duration;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Thin wrapper around any OpenAI-compatible /chat/completions and /audio/transcriptions
 * endpoint. Every AI feature in this service (summarize, translate, spam/moderation,
 * sentiment, smart replies) is a prompt built on top of the one chatComplete() primitive -
 * only voice-to-text needs a separate call, since transcription is a distinct multipart
 * endpoint, not a chat completion.
 */
@Component
public class OpenAiClient {

    private static final Logger log = LoggerFactory.getLogger(OpenAiClient.class);

    private final WebClient webClient;
    private final OpenAiProperties properties;

    public OpenAiClient(WebClient openAiWebClient, OpenAiProperties properties) {
        this.webClient = openAiWebClient;
        this.properties = properties;
    }

    public String chatComplete(String systemPrompt, String userPrompt) {
        return chatComplete(systemPrompt, userPrompt, 0.3);
    }

    public String chatComplete(String systemPrompt, String userPrompt, double temperature) {
        ChatCompletionRequest request = new ChatCompletionRequest(properties.getChatModel(),
                List.of(ChatMessage.system(systemPrompt), ChatMessage.user(userPrompt)), temperature);
        try {
            ChatCompletionResponse response = webClient.post()
                    .uri("/chat/completions")
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(ChatCompletionResponse.class)
                    .block(Duration.ofSeconds(properties.getTimeoutSeconds()));

            String content = response == null ? null : response.firstContent();
            if (content == null) {
                throw new AiServiceUnavailableException("The AI provider returned an empty completion", null);
            }
            return content;
        } catch (AiServiceUnavailableException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Chat completion call failed: {}", e.getMessage());
            throw new AiServiceUnavailableException("The AI provider is unavailable", e);
        }
    }

    public String transcribe(byte[] audioBytes, String filename, String contentType) {
        MultipartBodyBuilder builder = new MultipartBodyBuilder();
        builder.part("file", new ByteArrayResource(audioBytes) {
            @Override
            public String getFilename() {
                return filename;
            }
        }).contentType(MediaType.parseMediaType(contentType != null ? contentType : "application/octet-stream"));
        builder.part("model", properties.getTranscriptionModel());

        try {
            TranscriptionResponse response = webClient.post()
                    .uri("/audio/transcriptions")
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .bodyValue(builder.build())
                    .retrieve()
                    .bodyToMono(TranscriptionResponse.class)
                    .block(Duration.ofSeconds(properties.getTimeoutSeconds()));

            if (response == null || response.text() == null) {
                throw new AiServiceUnavailableException("The AI provider returned an empty transcription", null);
            }
            return response.text();
        } catch (AiServiceUnavailableException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Transcription call failed: {}", e.getMessage());
            throw new AiServiceUnavailableException("The AI provider is unavailable", e);
        }
    }
}
