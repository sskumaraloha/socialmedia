package com.socialmedia.auth.exception;

import com.socialmedia.common.exception.ConflictException;

public class EmailAlreadyRegisteredException extends ConflictException {

    public EmailAlreadyRegisteredException(String email) {
        super("An account with email '" + email + "' already exists");
    }
}
