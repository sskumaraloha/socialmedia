package com.socialmedia.media.domain;

import com.socialmedia.common.jpa.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "media_assets")
public class MediaAsset extends BaseEntity {

    @Column(nullable = false)
    private UUID ownerId;

    @Column(nullable = false, length = 500)
    private String originalFilename;

    @Column(nullable = false, length = 200)
    private String mimeType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MediaKind kind;

    private Long sizeBytes;

    @Column(nullable = false, length = 1000)
    private String storageKey;

    @Column(length = 1000)
    private String thumbnailKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MediaStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private VirusScanStatus virusScanStatus;

    /** The S3 multipart upload id - present while UPLOADING, cleared once completed/aborted. */
    @Column(length = 500)
    private String uploadId;

    @Column(nullable = false)
    private boolean transcoded;

    @Column(length = 500)
    private String failureReason;

    protected MediaAsset() {
    }

    public MediaAsset(UUID ownerId, String originalFilename, String mimeType, String storageKey, String uploadId) {
        this.ownerId = ownerId;
        this.originalFilename = originalFilename;
        this.mimeType = mimeType;
        this.kind = MediaKind.fromMimeType(mimeType);
        this.storageKey = storageKey;
        this.uploadId = uploadId;
        this.status = MediaStatus.UPLOADING;
        this.virusScanStatus = VirusScanStatus.PENDING;
    }

    public UUID getOwnerId() {
        return ownerId;
    }

    public String getOriginalFilename() {
        return originalFilename;
    }

    public String getMimeType() {
        return mimeType;
    }

    public MediaKind getKind() {
        return kind;
    }

    public Long getSizeBytes() {
        return sizeBytes;
    }

    public void setSizeBytes(Long sizeBytes) {
        this.sizeBytes = sizeBytes;
    }

    public String getStorageKey() {
        return storageKey;
    }

    public String getThumbnailKey() {
        return thumbnailKey;
    }

    public void setThumbnailKey(String thumbnailKey) {
        this.thumbnailKey = thumbnailKey;
    }

    public MediaStatus getStatus() {
        return status;
    }

    public VirusScanStatus getVirusScanStatus() {
        return virusScanStatus;
    }

    public void setVirusScanStatus(VirusScanStatus virusScanStatus) {
        this.virusScanStatus = virusScanStatus;
    }

    public String getUploadId() {
        return uploadId;
    }

    public boolean isTranscoded() {
        return transcoded;
    }

    public void setTranscoded(boolean transcoded) {
        this.transcoded = transcoded;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public void markUploaded() {
        this.status = MediaStatus.PROCESSING;
        this.uploadId = null;
    }

    public void markReady() {
        this.status = MediaStatus.READY;
    }

    public void markFailed(String reason) {
        this.status = MediaStatus.FAILED;
        this.failureReason = reason;
    }

    public void markQuarantined() {
        this.status = MediaStatus.QUARANTINED;
        this.virusScanStatus = VirusScanStatus.INFECTED;
    }
}
