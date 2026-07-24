package com.socialmedia.auth.exception;

import com.socialmedia.common.exception.BusinessException;
import org.springframework.http.HttpStatus;

public class AccountLockedException extends BusinessException {

    public AccountLockedException() {
        super(HttpStatus.LOCKED, "ACCOUNT_LOCKED", "Account is temporarily locked due to too many failed login attempts");
    }
}
