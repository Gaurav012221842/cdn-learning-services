package com.learningplatform.media.storage;

import com.learningplatform.common.exception.BusinessException;
import com.learningplatform.media.dto.CompletedPartDTO;
import com.learningplatform.media.dto.InitiateMultipartResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.UploadPartPresignRequest;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class R2MultipartService {

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;

    @Value("${cloudflare.r2.bucket-name:learning-platform}")
    private String bucketName;

    @Value("${cloudflare.r2.endpoint:}")
    private String r2Endpoint;

    @Value("${app.base-url:http://localhost:3000}")
    private String appBaseUrl;

    public String createMultipartUpload(String objectKey, String contentType) {
        try {
            if (isLocalMock()) {
                return "mock-upload-" + System.currentTimeMillis();
            }

            CreateMultipartUploadRequest request = CreateMultipartUploadRequest.builder()
                    .bucket(bucketName)
                    .key(objectKey)
                    .contentType(contentType != null && !contentType.isBlank() ? contentType : "video/mp4")
                    .build();

            CreateMultipartUploadResponse response = s3Client.createMultipartUpload(request);
            log.info("Initiated R2 multipart upload for key '{}' -> uploadId: {}", objectKey, response.uploadId());
            return response.uploadId();
        } catch (Exception e) {
            log.error("Failed to initiate R2 multipart upload for {}: {}", objectKey, e.getMessage(), e);
            throw new BusinessException("Could not initiate multipart upload in Cloudflare R2: " + e.getMessage());
        }
    }

    public String generatePresignedPartUrl(String objectKey, String uploadId, int partNumber) {
        try {
            if (isLocalMock()) {
                String baseUrl = appBaseUrl.endsWith("/") ? appBaseUrl.substring(0, appBaseUrl.length() - 1) : appBaseUrl;
                return baseUrl + "/api/v1/media/upload-direct?objectKey=" + objectKey + "&part=" + partNumber + "&uploadId=" + uploadId;
            }

            UploadPartRequest uploadPartRequest = UploadPartRequest.builder()
                    .bucket(bucketName)
                    .key(objectKey)
                    .uploadId(uploadId)
                    .partNumber(partNumber)
                    .build();

            UploadPartPresignRequest presignRequest = UploadPartPresignRequest.builder()
                    .signatureDuration(Duration.ofMinutes(15))
                    .uploadPartRequest(uploadPartRequest)
                    .build();

            return s3Presigner.presignUploadPart(presignRequest).url().toString();
        } catch (Exception e) {
            log.error("Failed generating presigned part URL for {} part #{}: {}", objectKey, partNumber, e.getMessage());
            throw new BusinessException("Could not generate presigned part URL: " + e.getMessage());
        }
    }

    public String uploadPartDirect(String objectKey, String uploadId, int partNumber, byte[] chunkData) {
        try {
            if (isLocalMock() || chunkData == null) {
                return "mock-etag-" + partNumber + "-" + System.currentTimeMillis();
            }

            UploadPartRequest uploadPartRequest = UploadPartRequest.builder()
                    .bucket(bucketName)
                    .key(objectKey)
                    .uploadId(uploadId)
                    .partNumber(partNumber)
                    .build();

            UploadPartResponse response = s3Client.uploadPart(uploadPartRequest, RequestBody.fromBytes(chunkData));
            return cleanETag(response.eTag());
        } catch (Exception e) {
            log.error("Failed uploading chunk for {} part #{}: {}", objectKey, partNumber, e.getMessage(), e);
            throw new BusinessException("Could not upload video chunk to R2: " + e.getMessage());
        }
    }

    public List<InitiateMultipartResponse.UploadedPartSummary> listUploadedParts(String objectKey, String uploadId) {
        List<InitiateMultipartResponse.UploadedPartSummary> result = new ArrayList<>();
        try {
            if (isLocalMock()) {
                return result;
            }

            ListPartsRequest request = ListPartsRequest.builder()
                    .bucket(bucketName)
                    .key(objectKey)
                    .uploadId(uploadId)
                    .build();

            ListPartsResponse response = s3Client.listParts(request);
            if (response.hasParts()) {
                result = response.parts().stream()
                        .map(p -> InitiateMultipartResponse.UploadedPartSummary.builder()
                                .partNumber(p.partNumber())
                                .eTag(cleanETag(p.eTag()))
                                .size(p.size())
                                .build())
                        .sorted(Comparator.comparing(InitiateMultipartResponse.UploadedPartSummary::getPartNumber))
                        .collect(Collectors.toList());
            }
        } catch (NoSuchUploadException e) {
            log.warn("Upload ID {} not found in R2 for listing parts.", uploadId);
        } catch (Exception e) {
            log.warn("Could not list R2 parts for upload {}: {}", uploadId, e.getMessage());
        }
        return result;
    }

    public void completeMultipartUpload(String objectKey, String uploadId, List<CompletedPartDTO> parts) {
        try {
            if (isLocalMock()) {
                log.info("Mock completed multipart upload for key: {}", objectKey);
                return;
            }

            List<CompletedPart> completedParts = new ArrayList<>();

            // 1. First attempt: Query authoritative parts directly from R2
            try {
                ListPartsRequest listPartsRequest = ListPartsRequest.builder()
                        .bucket(bucketName)
                        .key(objectKey)
                        .uploadId(uploadId)
                        .build();

                ListPartsResponse listPartsResponse = s3Client.listParts(listPartsRequest);
                if (listPartsResponse.hasParts() && !listPartsResponse.parts().isEmpty()) {
                    completedParts = listPartsResponse.parts().stream()
                            .map(p -> CompletedPart.builder()
                                    .partNumber(p.partNumber())
                                    .eTag(p.eTag())
                                    .build())
                            .sorted(Comparator.comparing(CompletedPart::partNumber))
                            .collect(Collectors.toList());
                    log.info("Loaded {} authoritative uploaded parts from R2 for completion.", completedParts.size());
                }
            } catch (Exception listErr) {
                log.warn("Could not query R2 listParts, falling back to payload parts: {}", listErr.getMessage());
            }

            // 2. Fallback: Use client-submitted parts if R2 listParts was empty
            if (completedParts.isEmpty() && parts != null) {
                completedParts = parts.stream()
                        .map(p -> CompletedPart.builder()
                                .partNumber(p.getPartNumber())
                                .eTag(p.getEtag() != null && !p.getEtag().startsWith("\"") ? "\"" + p.getEtag() + "\"" : p.getEtag())
                                .build())
                        .sorted(Comparator.comparing(CompletedPart::partNumber))
                        .collect(Collectors.toList());
            }

            CompletedMultipartUpload completedMultipartUpload = CompletedMultipartUpload.builder()
                    .parts(completedParts)
                    .build();

            CompleteMultipartUploadRequest request = CompleteMultipartUploadRequest.builder()
                    .bucket(bucketName)
                    .key(objectKey)
                    .uploadId(uploadId)
                    .multipartUpload(completedMultipartUpload)
                    .build();

            s3Client.completeMultipartUpload(request);
            log.info("Successfully completed R2 multipart upload for objectKey: {}", objectKey);
        } catch (Exception e) {
            log.error("Failed to complete R2 multipart upload for {}: {}", objectKey, e.getMessage(), e);
            throw new BusinessException("Failed to complete multipart video upload in R2: " + e.getMessage());
        }
    }

    public void abortMultipartUpload(String objectKey, String uploadId) {
        try {
            if (isLocalMock()) {
                return;
            }

            AbortMultipartUploadRequest request = AbortMultipartUploadRequest.builder()
                    .bucket(bucketName)
                    .key(objectKey)
                    .uploadId(uploadId)
                    .build();

            s3Client.abortMultipartUpload(request);
            log.info("Aborted R2 multipart upload for objectKey: {} and uploadId: {}", objectKey, uploadId);
        } catch (Exception e) {
            log.warn("Failed to abort multipart upload {}: {}", uploadId, e.getMessage());
        }
    }

    private String cleanETag(String etag) {
        if (etag == null) return "";
        return etag.replaceAll("\"", "").trim();
    }

    private boolean isLocalMock() {
        return r2Endpoint == null || r2Endpoint.contains("account-id") || r2Endpoint.contains("localhost");
    }
}
