package com.socialmedia.media.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * Decides how a client reaches an object's bytes: a CDN URL when this deployment has one
 * fronting the bucket (app.media.cdn.base-url set - the CDN handles its own caching/edge
 * auth), or a time-limited presigned S3 URL otherwise. Callers never need to know which.
 */
@Service
public class MediaUrlService {

    private final String cdnBaseUrl;
    private final S3StorageService storageService;

    public MediaUrlService(@Value("${app.media.cdn.base-url:}") String cdnBaseUrl, S3StorageService storageService) {
        this.cdnBaseUrl = cdnBaseUrl;
        this.storageService = storageService;
    }

    public String accessUrlFor(String storageKey) {
        if (storageKey == null) {
            return null;
        }
        if (StringUtils.hasText(cdnBaseUrl)) {
            return cdnBaseUrl.replaceAll("/$", "") + "/" + storageKey;
        }
        return storageService.presignGetObject(storageKey);
    }
}
