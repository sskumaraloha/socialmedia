package com.socialmedia.notification.service;

import java.util.UUID;

public interface PushSender {

    /** Sends to every device token currently registered for the user - a no-op if there are none. */
    void send(UUID userId, String title, String body);
}
