package com.socialmedia.ai.exception;

import com.socialmedia.common.exception.BusinessException;
import org.springframework.http.HttpStatus;

public class AiServiceUnavailableException extends BusinessException {

    public AiServiceUnavailableException(String message, Throwable cause) {
        super(HttpStatus.SERVICE_UNAVAILABLE, "AI_SERVICE_UNAVAILABLE", message, cause);
    }
}
