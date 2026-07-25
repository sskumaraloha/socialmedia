package com.socialmedia.media.domain;

public enum MediaKind {
    IMAGE,
    VIDEO,
    AUDIO,
    DOCUMENT,
    OTHER;

    public static MediaKind fromMimeType(String mimeType) {
        if (mimeType == null) {
            return OTHER;
        }
        String type = mimeType.toLowerCase();
        if (type.startsWith("image/")) {
            return IMAGE;
        }
        if (type.startsWith("video/")) {
            return VIDEO;
        }
        if (type.startsWith("audio/")) {
            return AUDIO;
        }
        if (type.equals("application/pdf") || type.startsWith("application/msword")
                || type.startsWith("application/vnd.")) {
            return DOCUMENT;
        }
        return OTHER;
    }
}
