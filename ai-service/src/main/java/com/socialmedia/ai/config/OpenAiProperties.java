package com.socialmedia.ai.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Config for any OpenAI-compatible endpoint - OpenAI itself, Azure OpenAI, or a
 * self-hosted server (vLLM, Ollama, LM Studio, ...) that speaks the same /chat/completions
 * and /audio/transcriptions wire contract. */
@ConfigurationProperties(prefix = "app.ai.openai")
public class OpenAiProperties {

    private String baseUrl = "https://api.openai.com/v1";
    private String apiKey = "";
    private String chatModel = "gpt-4o-mini";
    private String transcriptionModel = "whisper-1";
    private int timeoutSeconds = 30;

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getChatModel() {
        return chatModel;
    }

    public void setChatModel(String chatModel) {
        this.chatModel = chatModel;
    }

    public String getTranscriptionModel() {
        return transcriptionModel;
    }

    public void setTranscriptionModel(String transcriptionModel) {
        this.transcriptionModel = transcriptionModel;
    }

    public int getTimeoutSeconds() {
        return timeoutSeconds;
    }

    public void setTimeoutSeconds(int timeoutSeconds) {
        this.timeoutSeconds = timeoutSeconds;
    }
}
