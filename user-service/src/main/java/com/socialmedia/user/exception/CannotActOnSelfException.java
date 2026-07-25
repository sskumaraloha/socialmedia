package com.socialmedia.user.exception;

import com.socialmedia.common.exception.BusinessException;
import org.springframework.http.HttpStatus;

public class CannotActOnSelfException extends BusinessException {

    public CannotActOnSelfException(String action) {
        super(HttpStatus.BAD_REQUEST, "CANNOT_ACT_ON_SELF", "You cannot " + action + " yourself");
    }
}
