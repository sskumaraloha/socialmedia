package com.socialmedia.message.domain;

public class MediaMetadata {

    private String url;
    private String mimeType;
    private Long sizeBytes;
    private Integer durationSeconds;

    protected MediaMetadata() {
    }

    public MediaMetadata(String url, String mimeType, Long sizeBytes, Integer durationSeconds) {
        this.url = url;
        this.mimeType = mimeType;
        this.sizeBytes = sizeBytes;
        this.durationSeconds = durationSeconds;
    }

    public String getUrl() {
        return url;
    }

    public String getMimeType() {
        return mimeType;
    }

    public Long getSizeBytes() {
        return sizeBytes;
    }

    public Integer getDurationSeconds() {
        return durationSeconds;
    }
}
