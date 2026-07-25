package com.socialmedia.media.service.impl;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import net.coobird.thumbnailator.Thumbnails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/** Pure-Java image resize/re-encode (Thumbnailator) - covers thumbnailing and image compression alike. */
@Service
public class ThumbnailService {

    private static final Logger log = LoggerFactory.getLogger(ThumbnailService.class);

    private final int width;
    private final int height;
    private final float quality;

    public ThumbnailService(@Value("${app.media.thumbnail.width:320}") int width,
            @Value("${app.media.thumbnail.height:320}") int height,
            @Value("${app.media.thumbnail.quality:0.75}") float quality) {
        this.width = width;
        this.height = height;
        this.quality = quality;
    }

    public byte[] generate(byte[] original) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            Thumbnails.of(new ByteArrayInputStream(original))
                    .size(width, height)
                    .outputQuality(quality)
                    .outputFormat("jpg")
                    .toOutputStream(out);
            return out.toByteArray();
        } catch (IOException e) {
            log.warn("Thumbnail generation failed: {}", e.getMessage());
            return null;
        }
    }
}
