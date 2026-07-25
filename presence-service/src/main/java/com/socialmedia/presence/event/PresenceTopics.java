package com.socialmedia.presence.event;

public final class PresenceTopics {

    // Published - the exact contract user-service's PresenceChangedConsumer already
    // expects (see its event.incoming.PresenceChangedEvent), agreed up front before
    // this producer existed.
    public static final String PRESENCE_CHANGED = "presence.changed.v1";

    private PresenceTopics() {
    }
}
