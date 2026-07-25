package com.socialmedia.media.service;

import com.socialmedia.media.dto.request.CompleteUploadRequest;
import com.socialmedia.media.dto.request.InitiateUploadRequest;
import com.socialmedia.media.dto.response.InitiateUploadResponse;
import com.socialmedia.media.dto.response.MediaAssetResponse;
import com.socialmedia.media.dto.response.PartUploadUrlResponse;
import com.socialmedia.media.dto.response.UploadedPartResponse;
import java.util.List;
import java.util.UUID;

public interface MediaService {

    InitiateUploadResponse initiateUpload(UUID ownerId, InitiateUploadRequest request);

    PartUploadUrlResponse presignPart(UUID mediaId, UUID ownerId, int partNumber);

    List<UploadedPartResponse> listUploadedParts(UUID mediaId, UUID ownerId);

    MediaAssetResponse completeUpload(UUID mediaId, UUID ownerId, CompleteUploadRequest request);

    void abortUpload(UUID mediaId, UUID ownerId);

    MediaAssetResponse getMedia(UUID mediaId);

    void deleteMedia(UUID mediaId, UUID ownerId);
}
