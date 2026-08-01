package com.socialmedia.ai.controller;

import com.socialmedia.ai.dto.request.MeetingSummaryRequest;
import com.socialmedia.ai.dto.request.ReportMessageRequest;
import com.socialmedia.ai.dto.request.SmartReplyRequest;
import com.socialmedia.ai.dto.request.SummarizeRequest;
import com.socialmedia.ai.dto.request.TextRequest;
import com.socialmedia.ai.dto.request.TranslateRequest;
import com.socialmedia.ai.dto.response.MeetingSummaryResponse;
import com.socialmedia.ai.dto.response.ModerationResponse;
import com.socialmedia.ai.dto.response.SentimentResponse;
import com.socialmedia.ai.dto.response.SmartReplyResponse;
import com.socialmedia.ai.dto.response.SpamCheckResponse;
import com.socialmedia.ai.dto.response.SummaryResponse;
import com.socialmedia.ai.dto.response.TranscriptionResponse;
import com.socialmedia.ai.dto.response.TranslateResponse;
import com.socialmedia.ai.service.AiService;
import jakarta.validation.Valid;
import java.io.IOException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/ai")
public class AiController {

    private final AiService aiService;

    public AiController(AiService aiService) {
        this.aiService = aiService;
    }

    @PostMapping("/summarize")
    public SummaryResponse summarize(@Valid @RequestBody SummarizeRequest request) {
        return aiService.summarize(request.text());
    }

    @PostMapping("/meeting-summary")
    public MeetingSummaryResponse summarizeMeeting(@Valid @RequestBody MeetingSummaryRequest request) {
        return aiService.summarizeMeeting(request.transcript());
    }

    @PostMapping("/smart-replies")
    public SmartReplyResponse smartReplies(@Valid @RequestBody SmartReplyRequest request) {
        int count = request.count() == null ? 3 : Math.max(1, Math.min(request.count(), 5));
        return aiService.smartReplies(request.conversationContext(), count);
    }

    @PostMapping("/translate")
    public TranslateResponse translate(@Valid @RequestBody TranslateRequest request) {
        return aiService.translate(request.text(), request.targetLanguage());
    }

    @PostMapping("/spam-check")
    public SpamCheckResponse detectSpam(@Valid @RequestBody TextRequest request) {
        return aiService.detectSpam(request.text());
    }

    @PostMapping("/moderate")
    public ModerationResponse moderate(@Valid @RequestBody TextRequest request) {
        return aiService.moderate(request.text());
    }

    /** Replaces the old automatic message.sent.v1 moderation consumer, which end-to-end
     * encryption made impossible - see AiTopics. */
    @PostMapping("/report-message")
    public ModerationResponse reportMessage(@Valid @RequestBody ReportMessageRequest request) {
        return aiService.reportMessage(request);
    }

    @PostMapping("/sentiment")
    public SentimentResponse analyzeSentiment(@Valid @RequestBody TextRequest request) {
        return aiService.analyzeSentiment(request.text());
    }

    @PostMapping("/transcribe")
    public TranscriptionResponse transcribe(@RequestParam("file") MultipartFile file) throws IOException {
        String text = aiService.transcribe(file.getBytes(), file.getOriginalFilename(), file.getContentType());
        return new TranscriptionResponse(text);
    }
}
