package com.socialmedia.media.service;

import com.socialmedia.media.config.S3Properties;
import com.socialmedia.media.dto.request.CompletedPartRequest;
import com.socialmedia.media.dto.response.UploadedPartResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.AbortMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CompleteMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CompletedMultipartUpload;
import software.amazon.awssdk.services.s3.model.CompletedPart;
import software.amazon.awssdk.services.s3.model.CreateMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.ListPartsRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.UploadPartRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.UploadPartPresignRequest;

/**
 * Thin wrapper around the S3 multipart-upload API - S3 (and MinIO, which speaks the same
 * protocol) already provides chunked, resumable uploads and presigned URLs natively, so
 * this deliberately doesn't reinvent either: CreateMultipartUpload/UploadPart/
 * CompleteMultipartUpload give chunking, and the incomplete-upload staying addressable by
 * uploadId until explicitly completed or aborted gives resumability (ListParts tells a
 * reconnecting client exactly which parts still need uploading).
 */
@Service
public class S3StorageService {

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final String bucket;
    private final Duration presignExpiry;

    public S3StorageService(S3Client s3Client, S3Presigner s3Presigner, S3Properties properties) {
        this.s3Client = s3Client;
        this.s3Presigner = s3Presigner;
        this.bucket = properties.getBucket();
        this.presignExpiry = Duration.ofMinutes(properties.getPresignExpiryMinutes());
    }

    public String createMultipartUpload(String key, String contentType) {
        return s3Client.createMultipartUpload(CreateMultipartUploadRequest.builder()
                .bucket(bucket).key(key).contentType(contentType).build()).uploadId();
    }

    public String presignUploadPart(String key, String uploadId, int partNumber) {
        UploadPartRequest partRequest = UploadPartRequest.builder()
                .bucket(bucket).key(key).uploadId(uploadId).partNumber(partNumber).build();
        UploadPartPresignRequest presignRequest = UploadPartPresignRequest.builder()
                .signatureDuration(presignExpiry)
                .uploadPartRequest(partRequest)
                .build();
        return s3Presigner.presignUploadPart(presignRequest).url().toString();
    }

    public Instant presignExpiryFromNow() {
        return Instant.now().plus(presignExpiry);
    }

    public List<UploadedPartResponse> listParts(String key, String uploadId) {
        return s3Client.listParts(ListPartsRequest.builder().bucket(bucket).key(key).uploadId(uploadId).build())
                .parts().stream()
                .map(part -> new UploadedPartResponse(part.partNumber(), part.eTag(), part.size()))
                .toList();
    }

    public long completeMultipartUpload(String key, String uploadId, List<CompletedPartRequest> parts) {
        List<CompletedPart> completedParts = parts.stream()
                .map(part -> CompletedPart.builder().partNumber(part.partNumber()).eTag(part.eTag()).build())
                .toList();
        s3Client.completeMultipartUpload(CompleteMultipartUploadRequest.builder()
                .bucket(bucket).key(key).uploadId(uploadId)
                .multipartUpload(CompletedMultipartUpload.builder().parts(completedParts).build())
                .build());
        return s3Client.headObject(HeadObjectRequest.builder().bucket(bucket).key(key).build()).contentLength();
    }

    public void abortMultipartUpload(String key, String uploadId) {
        s3Client.abortMultipartUpload(AbortMultipartUploadRequest.builder().bucket(bucket).key(key).uploadId(uploadId).build());
    }

    public byte[] getObject(String key) {
        return s3Client.getObjectAsBytes(GetObjectRequest.builder().bucket(bucket).key(key).build()).asByteArray();
    }

    public void putObject(String key, byte[] content, String contentType) {
        s3Client.putObject(PutObjectRequest.builder().bucket(bucket).key(key).contentType(contentType).build(),
                RequestBody.fromBytes(content));
    }

    public void deleteObject(String key) {
        s3Client.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(key).build());
    }

    public String presignGetObject(String key) {
        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(presignExpiry)
                .getObjectRequest(GetObjectRequest.builder().bucket(bucket).key(key).build())
                .build();
        return s3Presigner.presignGetObject(presignRequest).url().toString();
    }
}
