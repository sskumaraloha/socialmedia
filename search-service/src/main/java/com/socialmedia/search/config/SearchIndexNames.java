package com.socialmedia.search.config;

/**
 * There is deliberately no "messages" index any more. Message content became end-to-end
 * encrypted, so message.sent.v1 no longer carries (and message-service can no longer produce) a
 * plaintext excerpt to index - server-side message-content search is not a feature that was
 * dropped for convenience, it is one that E2E encryption makes impossible, exactly as it is in
 * Signal and WhatsApp. Searchable message content returns when there is content the server is
 * legitimately allowed to read: public channel/broadcast posts, which are not private 1:1
 * traffic (see the Channels phase on the roadmap).
 */
public final class SearchIndexNames {

    public static final String USERS = "users";
    public static final String CHATS = "chats";
    public static final String MEDIA = "media";

    private SearchIndexNames() {
    }
}
