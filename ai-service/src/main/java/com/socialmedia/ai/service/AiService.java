package com.socialmedia.ai.service;

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

    String transcribe(byte[] audioBytes, String filename, String contentType);
}
