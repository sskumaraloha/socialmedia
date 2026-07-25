package com.socialmedia.user.exception;

import com.socialmedia.common.exception.ConflictException;

public class UsernameAlreadyTakenException extends ConflictException {

    public UsernameAlreadyTakenException(String username) {
        super("Username '" + username + "' is already taken");
    }
}
