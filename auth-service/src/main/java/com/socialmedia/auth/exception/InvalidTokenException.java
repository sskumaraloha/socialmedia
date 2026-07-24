package com.socialmedia.auth.exception;

import com.socialmedia.common.exception.BusinessException;
import org.springframework.http.HttpStatus;

public class InvalidTokenException extends BusinessException {

    public InvalidTokenException(String message) {
        super(HttpStatus.UNAUTHORIZED, "INVALID_TOKEN", message);
    }
}
