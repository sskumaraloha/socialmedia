package com.socialmedia.notification.exception;

import com.socialmedia.common.exception.BusinessException;
import org.springframework.http.HttpStatus;

public class PushUnavailableException extends BusinessException {

    public PushUnavailableException(String message) {
        super(HttpStatus.SERVICE_UNAVAILABLE, "PUSH_UNAVAILABLE", message);
    }
}
