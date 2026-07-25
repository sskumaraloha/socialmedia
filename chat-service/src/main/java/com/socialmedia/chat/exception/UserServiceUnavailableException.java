package com.socialmedia.chat.exception;

import com.socialmedia.common.exception.BusinessException;
import org.springframework.http.HttpStatus;

public class UserServiceUnavailableException extends BusinessException {

    public UserServiceUnavailableException() {
        super(HttpStatus.SERVICE_UNAVAILABLE, "USER_SERVICE_UNAVAILABLE",
                "Could not verify invited users right now - please try again");
    }
}
