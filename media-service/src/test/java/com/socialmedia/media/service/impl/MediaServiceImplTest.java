package com.socialmedia.media.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.socialmedia.media.domain.MediaAsset;
import com.socialmedia.media.dto.request.CompleteUploadRequest;
import com.socialmedia.media.dto.request.CompletedPartRequest;
import com.socialmedia.media.dto.request.InitiateUploadRequest;
import com.socialmedia.media.event.MediaEventPublisher;
import com.socialmedia.media.exception.NotMediaOwnerException;
import com.socialmedia.media.mapper.MediaMapper;
import com.socialmedia.media.repository.MediaAssetRepository;
import com.socialmedia.media.service.MediaUrlService;
import com.socialmedia.media.service.S3StorageService;
import com.socialmedia.common.exception.ResourceNotFoundException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MediaServiceImplTest {

    @Mock private MediaAssetRepository mediaAssetRepository;
    @Mock private S3StorageService storageService;
    @Mock private MediaProcessingService processingService;
    @Mock private MediaEventPublisher eventPublisher;
    @Mock private MediaUrlService mediaUrlService;

    private MediaServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new MediaServiceImpl(mediaAssetRepository, storageService, processingService, eventPublisher,
                new MediaMapper(mediaUrlService));
        lenient().when(mediaAssetRepository.save(any(MediaAsset.class))).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(mediaUrlService.accessUrlFor(anyString())).thenAnswer(inv -> "https://cdn.example.com/" + inv.getArgument(0));
    }

    @Test
    void initiateUploadCreatesAnAssetAndStartsAnS3MultipartUpload() {
        UUID ownerId = UUID.randomUUID();
        when(storageService.createMultipartUpload(anyString(), eq("image/png"))).thenReturn("s3-upload-id");

        var response = service.initiateUpload(ownerId, new InitiateUploadRequest("photo.png", "image/png"));

        assertThat(response.uploadId()).isEqualTo("s3-upload-id");
        assertThat(response.recommendedPartSizeBytes()).isPositive();
        verify(mediaAssetRepository).save(any(MediaAsset.class));
    }

    @Test
    void presignPartRejectsANonOwner() {
        UUID ownerId = UUID.randomUUID();
        UUID otherUserId = UUID.randomUUID();
        MediaAsset asset = new MediaAsset(ownerId, "photo.png", "image/png", "key", "upload-id");
        when(mediaAssetRepository.findById(asset.getId())).thenReturn(Optional.of(asset));

        assertThatThrownBy(() -> service.presignPart(asset.getId(), otherUserId, 1))
                .isInstanceOf(NotMediaOwnerException.class);
    }

    @Test
    void completeUploadMarksTheAssetUploadedAndTriggersAsyncProcessing() {
        UUID ownerId = UUID.randomUUID();
        MediaAsset asset = new MediaAsset(ownerId, "photo.png", "image/png", "key", "upload-id");
        when(mediaAssetRepository.findById(asset.getId())).thenReturn(Optional.of(asset));
        when(storageService.completeMultipartUpload(eq("key"), eq("upload-id"), any())).thenReturn(2048L);

        var response = service.completeUpload(asset.getId(), ownerId,
                new CompleteUploadRequest(List.of(new CompletedPartRequest(1, "etag-1"))));

        assertThat(asset.getUploadId()).isNull();
        assertThat(asset.getSizeBytes()).isEqualTo(2048L);
        assertThat(response.status().name()).isEqualTo("PROCESSING");
        verify(eventPublisher).publishUploaded(asset);
        verify(processingService).processAsync(asset.getId());
    }

    @Test
    void completeUploadRejectsAnUploadThatWasAlreadyCompleted() {
        UUID ownerId = UUID.randomUUID();
        MediaAsset asset = new MediaAsset(ownerId, "photo.png", "image/png", "key", "upload-id");
        asset.markUploaded();
        when(mediaAssetRepository.findById(asset.getId())).thenReturn(Optional.of(asset));

        assertThatThrownBy(() -> service.completeUpload(asset.getId(), ownerId,
                new CompleteUploadRequest(List.of(new CompletedPartRequest(1, "etag-1")))))
                .isInstanceOf(com.socialmedia.common.exception.ConflictException.class);
    }

    @Test
    void abortUploadDeletesTheAssetAndTheS3MultipartUpload() {
        UUID ownerId = UUID.randomUUID();
        MediaAsset asset = new MediaAsset(ownerId, "photo.png", "image/png", "key", "upload-id");
        when(mediaAssetRepository.findById(asset.getId())).thenReturn(Optional.of(asset));

        service.abortUpload(asset.getId(), ownerId);

        verify(storageService).abortMultipartUpload("key", "upload-id");
        verify(mediaAssetRepository).delete(asset);
    }

    @Test
    void deleteMediaRejectsANonOwner() {
        UUID ownerId = UUID.randomUUID();
        UUID otherUserId = UUID.randomUUID();
        MediaAsset asset = new MediaAsset(ownerId, "photo.png", "image/png", "key", "upload-id");
        when(mediaAssetRepository.findById(asset.getId())).thenReturn(Optional.of(asset));

        assertThatThrownBy(() -> service.deleteMedia(asset.getId(), otherUserId))
                .isInstanceOf(NotMediaOwnerException.class);
        verify(mediaAssetRepository, never()).delete(any());
    }

    @Test
    void getMediaThrowsNotFoundForAnUnknownId() {
        UUID mediaId = UUID.randomUUID();
        when(mediaAssetRepository.findById(mediaId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getMedia(mediaId)).isInstanceOf(ResourceNotFoundException.class);
    }
}
