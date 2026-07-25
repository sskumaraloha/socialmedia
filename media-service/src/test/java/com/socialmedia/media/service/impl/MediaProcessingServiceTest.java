package com.socialmedia.media.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.socialmedia.media.domain.MediaAsset;
import com.socialmedia.media.domain.MediaStatus;
import com.socialmedia.media.domain.VirusScanStatus;
import com.socialmedia.media.event.MediaEventPublisher;
import com.socialmedia.media.repository.MediaAssetRepository;
import com.socialmedia.media.service.S3StorageService;
import com.socialmedia.media.service.VirusScanService;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MediaProcessingServiceTest {

    @Mock private MediaAssetRepository mediaAssetRepository;
    @Mock private S3StorageService storageService;
    @Mock private VirusScanService virusScanService;
    @Mock private ThumbnailService thumbnailService;
    @Mock private VideoCompressionService videoCompressionService;
    @Mock private MediaEventPublisher eventPublisher;

    private MediaProcessingService failOpenService() {
        return new MediaProcessingService(mediaAssetRepository, storageService, virusScanService, thumbnailService,
                videoCompressionService, eventPublisher, false);
    }

    private MediaProcessingService failClosedService() {
        return new MediaProcessingService(mediaAssetRepository, storageService, virusScanService, thumbnailService,
                videoCompressionService, eventPublisher, true);
    }

    @Test
    void cleanImageGeneratesAThumbnailAndBecomesReady() {
        MediaAsset asset = new MediaAsset(UUID.randomUUID(), "photo.png", "image/png", "key", null);
        byte[] content = new byte[] {1, 2, 3};
        when(storageService.getObject("key")).thenReturn(content);
        when(virusScanService.scan(content)).thenReturn(VirusScanStatus.CLEAN);
        when(thumbnailService.generate(content)).thenReturn(new byte[] {9, 9});

        failOpenService().processNow(asset);

        assertThat(asset.getStatus()).isEqualTo(MediaStatus.READY);
        assertThat(asset.getThumbnailKey()).isEqualTo("key-thumbnail.jpg");
        verify(storageService).putObject("key-thumbnail.jpg", new byte[] {9, 9}, "image/jpeg");
        verify(eventPublisher).publishProcessed(asset);
    }

    @Test
    void infectedContentIsQuarantinedAndDeletedFromStorage() {
        MediaAsset asset = new MediaAsset(UUID.randomUUID(), "eicar.txt", "text/plain", "key", null);
        byte[] content = new byte[] {1};
        when(storageService.getObject("key")).thenReturn(content);
        when(virusScanService.scan(content)).thenReturn(VirusScanStatus.INFECTED);

        failOpenService().processNow(asset);

        assertThat(asset.getStatus()).isEqualTo(MediaStatus.QUARANTINED);
        verify(storageService).deleteObject("key");
        verify(eventPublisher).publishQuarantined(asset);
        verify(eventPublisher, never()).publishProcessed(any());
    }

    @Test
    void scanUnavailableFailsOpenByDefault() {
        MediaAsset asset = new MediaAsset(UUID.randomUUID(), "photo.png", "image/png", "key", null);
        byte[] content = new byte[] {1};
        when(storageService.getObject("key")).thenReturn(content);
        when(virusScanService.scan(content)).thenReturn(VirusScanStatus.SCAN_UNAVAILABLE);
        when(thumbnailService.generate(content)).thenReturn(new byte[] {9});

        failOpenService().processNow(asset);

        assertThat(asset.getStatus()).isEqualTo(MediaStatus.READY);
    }

    @Test
    void scanUnavailableFailsClosedWhenConfigured() {
        MediaAsset asset = new MediaAsset(UUID.randomUUID(), "photo.png", "image/png", "key", null);
        byte[] content = new byte[] {1};
        when(storageService.getObject("key")).thenReturn(content);
        when(virusScanService.scan(content)).thenReturn(VirusScanStatus.SCAN_UNAVAILABLE);

        failClosedService().processNow(asset);

        assertThat(asset.getStatus()).isEqualTo(MediaStatus.FAILED);
        verify(eventPublisher, never()).publishProcessed(any());
    }

    @Test
    void videoCompressionFailureStillLeavesTheAssetReadyWithoutTranscoding() {
        MediaAsset asset = new MediaAsset(UUID.randomUUID(), "clip.mov", "video/quicktime", "key", null);
        byte[] content = new byte[] {1, 2};
        when(storageService.getObject("key")).thenReturn(content);
        when(virusScanService.scan(content)).thenReturn(VirusScanStatus.CLEAN);
        when(videoCompressionService.compress(content, "mov")).thenReturn(Optional.empty());

        failOpenService().processNow(asset);

        assertThat(asset.getStatus()).isEqualTo(MediaStatus.READY);
        assertThat(asset.isTranscoded()).isFalse();
        verify(storageService, never()).putObject(anyString(), any(byte[].class), anyString());
    }

    @Test
    void processAsyncNoOpsWhenTheAssetNoLongerExists() {
        UUID mediaId = UUID.randomUUID();
        when(mediaAssetRepository.findById(mediaId)).thenReturn(Optional.empty());

        failOpenService().processAsync(mediaId);

        verify(mediaAssetRepository, never()).save(any());
    }
}
