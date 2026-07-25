package com.socialmedia.media.exception;

import com.socialmedia.common.exception.ForbiddenException;

public class NotMediaOwnerException extends ForbiddenException {

    public NotMediaOwnerException() {
        super("You do not own this media asset");
    }
}
