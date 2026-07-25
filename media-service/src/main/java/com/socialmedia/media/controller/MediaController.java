package com.socialmedia.media.controller;

import com.socialmedia.media.dto.request.CompleteUploadRequest;
import com.socialmedia.media.dto.request.InitiateUploadRequest;
import com.socialmedia.media.dto.response.InitiateUploadResponse;
import com.socialmedia.media.dto.response.MediaAssetResponse;
import com.socialmedia.media.dto.response.PartUploadUrlResponse;
import com.socialmedia.media.dto.response.UploadedPartResponse;
import com.socialmedia.media.service.MediaService;
import com.socialmedia.common.security.CurrentUser;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/media")
public class MediaController {

    private final MediaService mediaService;

    public MediaController(MediaService mediaService) {
        this.mediaService = mediaService;
    }

    @PostMapping("/uploads")
    @ResponseStatus(HttpStatus.CREATED)
    public InitiateUploadResponse initiateUpload(@Valid @RequestBody InitiateUploadRequest request) {
        return mediaService.initiateUpload(CurrentUser.get().userId(), request);
    }

    @PostMapping("/uploads/{mediaId}/parts/{partNumber}")
    public PartUploadUrlResponse presignPart(@PathVariable UUID mediaId, @PathVariable int partNumber) {
        return mediaService.presignPart(mediaId, CurrentUser.get().userId(), partNumber);
    }

    @GetMapping("/uploads/{mediaId}/parts")
    public List<UploadedPartResponse> listUploadedParts(@PathVariable UUID mediaId) {
        return mediaService.listUploadedParts(mediaId, CurrentUser.get().userId());
    }

    @PostMapping("/uploads/{mediaId}/complete")
    public MediaAssetResponse completeUpload(@PathVariable UUID mediaId, @Valid @RequestBody CompleteUploadRequest request) {
        return mediaService.completeUpload(mediaId, CurrentUser.get().userId(), request);
    }

    @PostMapping("/uploads/{mediaId}/abort")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void abortUpload(@PathVariable UUID mediaId) {
        mediaService.abortUpload(mediaId, CurrentUser.get().userId());
    }

    @GetMapping("/{mediaId}")
    public MediaAssetResponse getMedia(@PathVariable UUID mediaId) {
        return mediaService.getMedia(mediaId);
    }

    @DeleteMapping("/{mediaId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteMedia(@PathVariable UUID mediaId) {
        mediaService.deleteMedia(mediaId, CurrentUser.get().userId());
    }
}
