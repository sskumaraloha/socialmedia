package com.socialmedia.message.exception;

import com.socialmedia.common.exception.ForbiddenException;

public class NotMessageAuthorException extends ForbiddenException {

    public NotMessageAuthorException(String action) {
        super("Only the sender can " + action + " this message");
    }
}
