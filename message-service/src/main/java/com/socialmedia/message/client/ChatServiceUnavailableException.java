package com.socialmedia.message.client;

import com.socialmedia.common.exception.BusinessException;
import org.springframework.http.HttpStatus;

public class ChatServiceUnavailableException extends BusinessException {

    public ChatServiceUnavailableException() {
        super(HttpStatus.SERVICE_UNAVAILABLE, "CHAT_SERVICE_UNAVAILABLE",
                "Could not verify chat membership right now - chat-service is unreachable");
    }
}
