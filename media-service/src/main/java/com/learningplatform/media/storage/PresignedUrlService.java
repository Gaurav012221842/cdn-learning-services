package com.learningplatform.media.storage;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class PresignedUrlService {

    private final S3Presigner s3Presigner;

    @Value("${cloudflare.r2.bucket-name}")
    private String bucketName;

    @Value("${cloudflare.r2.endpoint}")
    private String r2Endpoint;

    @Value("${app.base-url:http://localhost:8080}")
    private String appBaseUrl;

    @Value("${cloudflare.r2.cdn-url}")
    private String cdnUrl;

    public String generateUploadPresignedUrl(String objectKey) {
        if (r2Endpoint == null || r2Endpoint.contains("account-id") || r2Endpoint.contains("localhost")) {
            String baseUrl = appBaseUrl.endsWith("/") ? appBaseUrl.substring(0, appBaseUrl.length() - 1) : appBaseUrl;
            return baseUrl + "/api/v1/media/upload-direct?objectKey=" + objectKey;
        }

        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(objectKey)
                .build();

        PresignedPutObjectRequest presignedRequest = s3Presigner.presignPutObject(
                builder -> builder
                        .signatureDuration(Duration.ofMinutes(15))
                        .putObjectRequest(putObjectRequest)
        );

        return presignedRequest.url().toString();
    }

    public String generateDownloadPresignedUrl(String objectKey) {
        return cdnUrl.endsWith("/") ? cdnUrl + objectKey : cdnUrl + "/" + objectKey;
    }
}
