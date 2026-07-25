package com.socialmedia.chat.exception;

import com.socialmedia.common.exception.ForbiddenException;

public class InsufficientChatPermissionException extends ForbiddenException {

    public InsufficientChatPermissionException(String action) {
        super("You do not have permission to " + action);
    }
}
