package com.socialmedia.common.exception;

import java.time.Instant;
import java.util.List;

/**
 * Consistent error payload returned by every service in the platform.
 */
public record ApiError(
        Instant timestamp,
        int status,
        String error,
        String errorCode,
        String message,
        String path,
        String traceId,
        List<FieldValidationError> fieldErrors
) {

    public record FieldValidationError(String field, String message) {
    }
}
