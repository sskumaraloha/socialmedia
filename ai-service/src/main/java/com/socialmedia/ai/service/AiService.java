package com.socialmedia.ai.service;

import com.socialmedia.ai.dto.request.ReportMessageRequest;
import com.socialmedia.ai.dto.response.MeetingSummaryResponse;
import com.socialmedia.ai.dto.response.ModerationResponse;
import com.socialmedia.ai.dto.response.SentimentResponse;
import com.socialmedia.ai.dto.response.SmartReplyResponse;
import com.socialmedia.ai.dto.response.SpamCheckResponse;
import com.socialmedia.ai.dto.response.SummaryResponse;
import com.socialmedia.ai.dto.response.TranslateResponse;

public interface AiService {

    SummaryResponse summarize(String text);

    MeetingSummaryResponse summarizeMeeting(String transcript);

    SmartReplyResponse smartReplies(String conversationContext, int count);

    TranslateResponse translate(String text, String targetLanguage);

    SpamCheckResponse detectSpam(String text);

    ModerationResponse moderate(String text);

    SentimentResponse analyzeSentiment(String text);

    /** Moderates a message a user explicitly reported, and publishes ai.message.moderated.v1 so
     * the rest of the platform can act on the verdict. The reporting client supplies the
     * plaintext - see ReportMessageRequest. */
    ModerationResponse reportMessage(ReportMessageRequest request);

    String transcribe(byte[] audioBytes, String filename, String contentType);
}
