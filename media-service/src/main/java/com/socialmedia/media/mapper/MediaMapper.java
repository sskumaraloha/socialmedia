package com.socialmedia.media.mapper;

import com.socialmedia.media.domain.MediaAsset;
import com.socialmedia.media.dto.response.MediaAssetResponse;
import com.socialmedia.media.service.MediaUrlService;
import org.springframework.stereotype.Component;

@Component
public class MediaMapper {

    private final MediaUrlService mediaUrlService;

    public MediaMapper(MediaUrlService mediaUrlService) {
        this.mediaUrlService = mediaUrlService;
    }

    public MediaAssetResponse toResponse(MediaAsset asset) {
        return new MediaAssetResponse(
                asset.getId(),
                asset.getOwnerId(),
                asset.getOriginalFilename(),
                asset.getMimeType(),
                asset.getKind(),
                asset.getSizeBytes(),
                asset.getStatus(),
                asset.getVirusScanStatus(),
                asset.isTranscoded(),
                mediaUrlService.accessUrlFor(asset.getStorageKey()),
                mediaUrlService.accessUrlFor(asset.getThumbnailKey()),
                asset.getCreatedAt());
    }
}
