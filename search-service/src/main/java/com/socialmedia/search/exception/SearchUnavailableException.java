package com.socialmedia.search.exception;

import com.socialmedia.common.exception.BusinessException;
import org.springframework.http.HttpStatus;

public class SearchUnavailableException extends BusinessException {

    public SearchUnavailableException(Throwable cause) {
        super(HttpStatus.SERVICE_UNAVAILABLE, "SEARCH_UNAVAILABLE", "The search index is temporarily unavailable", cause);
    }
}
