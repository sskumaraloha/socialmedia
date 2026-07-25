package com.socialmedia.media.service.impl;

import com.socialmedia.media.domain.MediaAsset;
import com.socialmedia.media.domain.MediaKind;
import com.socialmedia.media.domain.VirusScanStatus;
import com.socialmedia.media.event.MediaEventPublisher;
import com.socialmedia.media.repository.MediaAssetRepository;
import com.socialmedia.media.service.S3StorageService;
import com.socialmedia.media.service.VirusScanService;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Runs off the request thread (@Async) so a client completing an upload isn't held open
 * for however long virus scanning / thumbnailing / transcoding takes - the asset sits in
 * PROCESSING until this finishes, and the client polls GET /media/{id} to see the result.
 */
@Service
public class MediaProcessingService {

    private static final Logger log = LoggerFactory.getLogger(MediaProcessingService.class);

    private final MediaAssetRepository mediaAssetRepository;
    private final S3StorageService storageService;
    private final VirusScanService virusScanService;
    private final ThumbnailService thumbnailService;
    private final VideoCompressionService videoCompressionService;
    private final MediaEventPublisher eventPublisher;
    private final boolean failClosedOnScanUnavailable;

    public MediaProcessingService(MediaAssetRepository mediaAssetRepository, S3StorageService storageService,
            VirusScanService virusScanService, ThumbnailService thumbnailService,
            VideoCompressionService videoCompressionService, MediaEventPublisher eventPublisher,
            @Value("${app.media.virus-scan.fail-closed:false}") boolean failClosedOnScanUnavailable) {
        this.mediaAssetRepository = mediaAssetRepository;
        this.storageService = storageService;
        this.virusScanService = virusScanService;
        this.thumbnailService = thumbnailService;
        this.videoCompressionService = videoCompressionService;
        this.eventPublisher = eventPublisher;
        this.failClosedOnScanUnavailable = failClosedOnScanUnavailable;
    }

    @Async
    public void processAsync(UUID mediaId) {
        mediaAssetRepository.findById(mediaId).ifPresent(asset -> {
            processNow(asset);
            mediaAssetRepository.save(asset);
        });
    }

    void processNow(MediaAsset asset) {
        byte[] content;
        try {
            content = storageService.getObject(asset.getStorageKey());
        } catch (Exception e) {
            log.error("Could not fetch uploaded object {} for processing: {}", asset.getStorageKey(), e.getMessage());
            asset.markFailed("Could not retrieve uploaded object: " + e.getMessage());
            return;
        }

        VirusScanStatus scanStatus = virusScanService.scan(content);
        asset.setVirusScanStatus(scanStatus);

        if (scanStatus == VirusScanStatus.INFECTED) {
            asset.markQuarantined();
            safeDelete(asset.getStorageKey());
            eventPublisher.publishQuarantined(asset);
            return;
        }
        if (scanStatus == VirusScanStatus.SCAN_UNAVAILABLE && failClosedOnScanUnavailable) {
            asset.markFailed("Virus scan unavailable and this deployment fails closed");
            return;
        }

        if (asset.getKind() == MediaKind.IMAGE) {
            generateThumbnail(asset, content);
        } else if (asset.getKind() == MediaKind.VIDEO) {
            compressVideo(asset, content);
        }

        asset.markReady();
        eventPublisher.publishProcessed(asset);
    }

    private void generateThumbnail(MediaAsset asset, byte[] content) {
        byte[] thumbnail = thumbnailService.generate(content);
        if (thumbnail == null) {
            return;
        }
        String thumbnailKey = asset.getStorageKey() + "-thumbnail.jpg";
        storageService.putObject(thumbnailKey, thumbnail, "image/jpeg");
        asset.setThumbnailKey(thumbnailKey);
    }

    private void compressVideo(MediaAsset asset, byte[] content) {
        String extension = extensionOf(asset.getOriginalFilename());
        Optional<byte[]> compressed = videoCompressionService.compress(content, extension);
        compressed.ifPresent(bytes -> {
            storageService.putObject(asset.getStorageKey(), bytes, "video/mp4");
            asset.setTranscoded(true);
        });
    }

    private String extensionOf(String filename) {
        int dot = filename.lastIndexOf('.');
        return dot >= 0 && dot < filename.length() - 1 ? filename.substring(dot + 1) : "mp4";
    }

    private void safeDelete(String key) {
        try {
            storageService.deleteObject(key);
        } catch (Exception e) {
            log.warn("Failed to delete quarantined object {}: {}", key, e.getMessage());
        }
    }
}
