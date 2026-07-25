package com.socialmedia.chat.exception;

import com.socialmedia.common.exception.ForbiddenException;

public class NotAChatMemberException extends ForbiddenException {

    public NotAChatMemberException() {
        super("You are not a member of this chat");
    }
}
