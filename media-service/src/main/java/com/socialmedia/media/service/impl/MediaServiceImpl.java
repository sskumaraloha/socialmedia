package com.socialmedia.media.service.impl;

import com.socialmedia.media.domain.MediaAsset;
import com.socialmedia.media.dto.request.CompleteUploadRequest;
import com.socialmedia.media.dto.request.InitiateUploadRequest;
import com.socialmedia.media.dto.response.InitiateUploadResponse;
import com.socialmedia.media.dto.response.MediaAssetResponse;
import com.socialmedia.media.dto.response.PartUploadUrlResponse;
import com.socialmedia.media.dto.response.UploadedPartResponse;
import com.socialmedia.media.event.MediaEventPublisher;
import com.socialmedia.media.exception.NotMediaOwnerException;
import com.socialmedia.media.mapper.MediaMapper;
import com.socialmedia.media.repository.MediaAssetRepository;
import com.socialmedia.media.service.MediaService;
import com.socialmedia.media.service.S3StorageService;
import com.socialmedia.common.exception.ResourceNotFoundException;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class MediaServiceImpl implements MediaService {

    private static final int RECOMMENDED_PART_SIZE_BYTES = 8 * 1024 * 1024;

    private final MediaAssetRepository mediaAssetRepository;
    private final S3StorageService storageService;
    private final MediaProcessingService processingService;
    private final MediaEventPublisher eventPublisher;
    private final MediaMapper mapper;

    public MediaServiceImpl(MediaAssetRepository mediaAssetRepository, S3StorageService storageService,
            MediaProcessingService processingService, MediaEventPublisher eventPublisher, MediaMapper mapper) {
        this.mediaAssetRepository = mediaAssetRepository;
        this.storageService = storageService;
        this.processingService = processingService;
        this.eventPublisher = eventPublisher;
        this.mapper = mapper;
    }

    @Override
    public InitiateUploadResponse initiateUpload(UUID ownerId, InitiateUploadRequest request) {
        String storageKey = ownerId + "/" + UUID.randomUUID() + "-" + request.originalFilename();
        String uploadId = storageService.createMultipartUpload(storageKey, request.mimeType());

        MediaAsset asset = new MediaAsset(ownerId, request.originalFilename(), request.mimeType(), storageKey, uploadId);
        mediaAssetRepository.save(asset);

        return new InitiateUploadResponse(asset.getId(), uploadId, RECOMMENDED_PART_SIZE_BYTES);
    }

    @Override
    @Transactional(readOnly = true)
    public PartUploadUrlResponse presignPart(UUID mediaId, UUID ownerId, int partNumber) {
        MediaAsset asset = requireOwnedUploadInProgress(mediaId, ownerId);
        String url = storageService.presignUploadPart(asset.getStorageKey(), asset.getUploadId(), partNumber);
        return new PartUploadUrlResponse(partNumber, url, storageService.presignExpiryFromNow());
    }

    @Override
    @Transactional(readOnly = true)
    public List<UploadedPartResponse> listUploadedParts(UUID mediaId, UUID ownerId) {
        MediaAsset asset = requireOwnedUploadInProgress(mediaId, ownerId);
        return storageService.listParts(asset.getStorageKey(), asset.getUploadId());
    }

    @Override
    public MediaAssetResponse completeUpload(UUID mediaId, UUID ownerId, CompleteUploadRequest request) {
        MediaAsset asset = requireOwnedUploadInProgress(mediaId, ownerId);

        long sizeBytes = storageService.completeMultipartUpload(asset.getStorageKey(), asset.getUploadId(), request.parts());
        asset.setSizeBytes(sizeBytes);
        asset.markUploaded();
        mediaAssetRepository.save(asset);

        eventPublisher.publishUploaded(asset);
        processingService.processAsync(asset.getId());

        return mapper.toResponse(asset);
    }

    @Override
    public void abortUpload(UUID mediaId, UUID ownerId) {
        MediaAsset asset = requireOwnedUploadInProgress(mediaId, ownerId);
        storageService.abortMultipartUpload(asset.getStorageKey(), asset.getUploadId());
        mediaAssetRepository.delete(asset);
    }

    @Override
    @Transactional(readOnly = true)
    public MediaAssetResponse getMedia(UUID mediaId) {
        return mapper.toResponse(requireAsset(mediaId));
    }

    @Override
    public void deleteMedia(UUID mediaId, UUID ownerId) {
        MediaAsset asset = requireAsset(mediaId);
        if (!asset.getOwnerId().equals(ownerId)) {
            throw new NotMediaOwnerException();
        }
        storageService.deleteObject(asset.getStorageKey());
        if (asset.getThumbnailKey() != null) {
            storageService.deleteObject(asset.getThumbnailKey());
        }
        mediaAssetRepository.delete(asset);
    }

    private MediaAsset requireAsset(UUID mediaId) {
        return mediaAssetRepository.findById(mediaId).orElseThrow(() -> new ResourceNotFoundException("Media asset", mediaId));
    }

    private MediaAsset requireOwnedUploadInProgress(UUID mediaId, UUID ownerId) {
        MediaAsset asset = requireAsset(mediaId);
        if (!asset.getOwnerId().equals(ownerId)) {
            throw new NotMediaOwnerException();
        }
        if (asset.getUploadId() == null) {
            throw new com.socialmedia.common.exception.ConflictException("This upload has already been completed or aborted");
        }
        return asset;
    }
}
