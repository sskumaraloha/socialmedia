package com.socialmedia.notification.exception;

import com.socialmedia.common.exception.BusinessException;
import org.springframework.http.HttpStatus;

public class UnknownNotificationTemplateException extends BusinessException {

    public UnknownNotificationTemplateException(String templateKey) {
        super(HttpStatus.BAD_REQUEST, "UNKNOWN_TEMPLATE", "No notification template registered for key: " + templateKey);
    }
}
