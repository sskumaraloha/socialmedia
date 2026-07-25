package com.socialmedia.common.exception;

import org.springframework.http.HttpStatus;

/** The caller is authenticated but lacks permission for the action (e.g. not a chat admin). */
public class ForbiddenException extends BusinessException {

    public ForbiddenException(String message) {
        super(HttpStatus.FORBIDDEN, "FORBIDDEN", message);
    }
}
