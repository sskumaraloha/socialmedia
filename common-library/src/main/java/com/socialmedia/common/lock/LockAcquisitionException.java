package com.socialmedia.common.lock;

import com.socialmedia.common.exception.BusinessException;
import org.springframework.http.HttpStatus;

/** Thrown when a distributed lock could not be acquired within the caller's wait budget -
 * another request is already processing the same key. */
public class LockAcquisitionException extends BusinessException {

    public LockAcquisitionException(String key) {
        super(HttpStatus.CONFLICT, "RESOURCE_LOCKED",
                "Another request is already processing '" + key + "' - please retry");
    }
}
