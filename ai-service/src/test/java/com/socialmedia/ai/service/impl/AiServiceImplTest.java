package com.socialmedia.ai.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.socialmedia.ai.client.OpenAiClient;
import com.socialmedia.ai.dto.request.ReportMessageRequest;
import com.socialmedia.ai.dto.response.MeetingSummaryResponse;
import com.socialmedia.ai.dto.response.ModerationResponse;
import com.socialmedia.ai.dto.response.SentimentResponse;
import com.socialmedia.ai.dto.response.SmartReplyResponse;
import com.socialmedia.ai.dto.response.SpamCheckResponse;
import com.socialmedia.ai.event.AiEventPublisher;
import com.socialmedia.ai.event.outgoing.MessageModeratedEvent;
import com.socialmedia.ai.exception.AiServiceUnavailableException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import java.util.UUID;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

@ExtendWith(MockitoExtension.class)
class AiServiceImplTest {

    @Mock private OpenAiClient openAiClient;
    @Mock private StringRedisTemplate redisTemplate;
    @Mock private ValueOperations<String, String> valueOperations;
    @Mock private AiEventPublisher eventPublisher;

    private AiServiceImpl service;

    @BeforeEach
    void setUp() {
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        service = new AiServiceImpl(openAiClient, new ObjectMapper(), redisTemplate, eventPublisher, 60);
    }

    @Test
    void summarizeCallsTheModelOnACacheMissAndCachesTheResult() {
        when(valueOperations.get(anyString())).thenReturn(null);
        when(openAiClient.chatComplete(anyString(), eq("hello world"))).thenReturn("A short summary.");

        var response = service.summarize("hello world");

        assertThat(response.summary()).isEqualTo("A short summary.");
        verify(valueOperations).set(anyString(), eq("A short summary."), any());
    }

    @Test
    void summarizeReturnsTheCachedValueWithoutCallingTheModelAgain() {
        when(valueOperations.get(anyString())).thenReturn("Cached summary.");

        var response = service.summarize("hello world");

        assertThat(response.summary()).isEqualTo("Cached summary.");
        verify(openAiClient, never()).chatComplete(anyString(), anyString());
    }

    @Test
    void translateCachesByTextAndTargetLanguage() {
        when(valueOperations.get(anyString())).thenReturn(null);
        when(openAiClient.chatComplete(anyString(), eq("hello"))).thenReturn("hola");

        var response = service.translate("hello", "es");

        assertThat(response.translatedText()).isEqualTo("hola");
        verify(valueOperations).set(anyString(), eq("hola"), any());
    }

    @Test
    void detectSpamParsesTheModelsJsonResponse() {
        when(openAiClient.chatComplete(anyString(), anyString()))
                .thenReturn("{\"spam\": true, \"confidence\": 0.92, \"reason\": \"unsolicited link\"}");

        SpamCheckResponse response = service.detectSpam("buy cheap watches now!!!");

        assertThat(response.spam()).isTrue();
        assertThat(response.confidence()).isEqualTo(0.92);
        assertThat(response.reason()).isEqualTo("unsolicited link");
    }

    @Test
    void moderateParsesTheModelsJsonResponse() {
        when(openAiClient.chatComplete(anyString(), anyString()))
                .thenReturn("{\"flagged\": false, \"categories\": [], \"reason\": \"none\"}");

        ModerationResponse response = service.moderate("have a nice day");

        assertThat(response.flagged()).isFalse();
        assertThat(response.categories()).isEmpty();
    }

    @Test
    void analyzeSentimentParsesTheModelsJsonResponse() {
        when(openAiClient.chatComplete(anyString(), anyString()))
                .thenReturn("{\"sentiment\": \"POSITIVE\", \"score\": 0.87}");

        SentimentResponse response = service.analyzeSentiment("what a great day!");

        assertThat(response.sentiment()).isEqualTo("POSITIVE");
        assertThat(response.score()).isEqualTo(0.87);
    }

    @Test
    void summarizeMeetingParsesAStructuredJsonResponse() {
        when(openAiClient.chatComplete(anyString(), anyString()))
                .thenReturn("{\"summary\": \"Team sync\", \"keyPoints\": [\"launched v2\"], \"actionItems\": [\"file report\"]}");

        MeetingSummaryResponse response = service.summarizeMeeting("we discussed the v2 launch...");

        assertThat(response.summary()).isEqualTo("Team sync");
        assertThat(response.keyPoints()).containsExactly("launched v2");
        assertThat(response.actionItems()).containsExactly("file report");
    }

    @Test
    void smartRepliesParsesAJsonArrayOfSuggestions() {
        when(openAiClient.chatComplete(anyString(), anyString(), eq(0.7)))
                .thenReturn("[\"Sounds good!\", \"I'll check and get back to you.\", \"Thanks!\"]");

        SmartReplyResponse response = service.smartReplies("Are we still on for lunch?", 3);

        assertThat(response.suggestions()).hasSize(3).contains("Sounds good!");
    }

    @Test
    void malformedModelResponseIsWrappedAsAiServiceUnavailable() {
        when(openAiClient.chatComplete(anyString(), anyString())).thenReturn("not valid json");

        assertThatThrownBy(() -> service.detectSpam("hello")).isInstanceOf(AiServiceUnavailableException.class);
    }

    @Test
    void transcribeDelegatesDirectlyToTheClient() {
        byte[] audio = new byte[] {1, 2, 3};
        when(openAiClient.transcribe(audio, "clip.wav", "audio/wav")).thenReturn("hello there");

        String text = service.transcribe(audio, "clip.wav", "audio/wav");

        assertThat(text).isEqualTo("hello there");
    }

    @Test
    void reportMessageModeratesTheClientSuppliedPlaintextAndPublishesTheVerdict() {
        when(openAiClient.chatComplete(anyString(), eq("abusive text")))
                .thenReturn("{\"flagged\": true, \"categories\": [\"harassment\"], \"reason\": \"targeted abuse\"}")
                .thenReturn("{\"spam\": false, \"confidence\": 0.1, \"reason\": \"not spam\"}");

        UUID messageId = UUID.randomUUID();
        UUID chatId = UUID.randomUUID();
        UUID senderId = UUID.randomUUID();

        ModerationResponse response = service.reportMessage(
                new ReportMessageRequest(messageId, chatId, senderId, "abusive text"));

        assertThat(response.flagged()).isTrue();
        assertThat(response.categories()).contains("harassment");

        ArgumentCaptor<MessageModeratedEvent> captor = ArgumentCaptor.forClass(MessageModeratedEvent.class);
        verify(eventPublisher).publishMessageModerated(captor.capture());
        MessageModeratedEvent published = captor.getValue();
        assertThat(published.messageId()).isEqualTo(messageId);
        assertThat(published.senderId()).isEqualTo(senderId);
        assertThat(published.flagged()).isTrue();
        assertThat(published.reason()).isEqualTo("targeted abuse");
    }
}
