package com.socialmedia.ai.event;

public final class AiTopics {

    // Nothing is consumed any more. This service used to subscribe to message.sent.v1 and
    // auto-moderate every message's content preview; end-to-end encryption removed that
    // preview from the event (and the server's ability to produce one at all), so automatic
    // server-side moderation of private messages is now impossible by construction - the same
    // tradeoff Signal and WhatsApp make. Abuse handling moved to the client-initiated report
    // flow on AiController (POST /api/v1/ai/report-message), where the reporting user's own
    // client supplies the plaintext it already holds.

    // Published
    public static final String MESSAGE_MODERATED = "ai.message.moderated.v1";

    private AiTopics() {
    }
}
