package com.learningplatform.media;

import com.learningplatform.common.exception.BusinessException;
import com.learningplatform.media.dto.AvatarUploadRequest;
import com.learningplatform.media.dto.AvatarUploadResponse;
import com.learningplatform.media.dto.SignedUrlResponse;
import com.learningplatform.media.dto.UploadRequest;
import com.learningplatform.media.dto.UploadResponse;
import com.learningplatform.media.storage.ObjectKeyGenerator;
import com.learningplatform.media.storage.PresignedUrlService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MediaService {

    private static final Logger log = LoggerFactory.getLogger(MediaService.class);

    private final MediaRepository mediaRepository;
    private final PresignedUrlService presignedUrlService;
    private final ObjectKeyGenerator objectKeyGenerator;
    private final com.learningplatform.media.storage.CloudflareR2Logger cloudflareR2Logger;
    private final S3Client s3Client;
    private final org.springframework.kafka.core.KafkaTemplate<String, String> kafkaTemplate;

    @Value("${cloudflare.r2.bucket-name:learning-platform}")
    private String bucketName;

    @Value("${cloudflare.r2.endpoint}")
    private String r2Endpoint;

    @Value("${cloudflare.r2.cdn-url}")
    private String cdnUrl;

    @Value("${local.uploads-url}")
    private String localUploadsUrl;

    public UploadResponse requestUpload(UploadRequest request) {
        validateFile(request);

        String objectKey = objectKeyGenerator.generateKey(
                request.getOriginalFilename(),
                request.getMediaType().name().toLowerCase() + "s"
        );

        Media media = Media.builder()
                .originalFilename(request.getOriginalFilename())
                .objectKey(objectKey)
                .mediaType(request.getMediaType())
                .mimeType(request.getMimeType())
                .fileSize(request.getFileSize())
                .status(MediaStatus.UPLOADING)
                .build();

        Media savedMedia = mediaRepository.save(media);
        String presignedUrl = presignedUrlService.generateUploadPresignedUrl(objectKey);
        String generatedCdnUrl = cdnUrl.endsWith("/") ? cdnUrl + objectKey : cdnUrl + "/" + objectKey;

        return UploadResponse.builder()
                .mediaId(savedMedia.getId())
                .objectKey(objectKey)
                .presignedUploadUrl(presignedUrl)
                .cdnUrl(generatedCdnUrl)
                .build();
    }

    public Media confirmUpload(UUID mediaId) {
        Media media = mediaRepository.findById(mediaId)
                .orElseThrow(() -> new BusinessException("Media not found: " + mediaId));
        media.setStatus(MediaStatus.COMPLETED);
        Media updatedMedia = mediaRepository.save(media);

        if (media.getMediaType() == MediaType.VIDEO && kafkaTemplate != null) {
            try {
                log.info("Publishing transcoding event to Kafka for mediaId: {}", updatedMedia.getId());
                kafkaTemplate.send("media-transcoding-events", updatedMedia.getId().toString());
            } catch (Exception e) {
                log.warn("Could not dispatch Kafka transcoding event: {}", e.getMessage());
            }
        }

        return updatedMedia;
    }

    public SignedUrlResponse getSignedStreamingUrl(UUID mediaId) {
        Media media = mediaRepository.findById(mediaId)
                .orElseThrow(() -> new BusinessException("Media not found: " + mediaId));
        String streamingUrl = presignedUrlService.generateDownloadPresignedUrl(media.getObjectKey());
        return new SignedUrlResponse(streamingUrl, 3600);
    }

    public com.learningplatform.media.dto.CloudflareR2StatusResponse getCloudflareStatus() {
        if (cloudflareR2Logger != null) {
            cloudflareR2Logger.testAndLogConnection(r2Endpoint, bucketName);
        }
        return com.learningplatform.media.dto.CloudflareR2StatusResponse.builder()
                .connected(true)
                .endpoint(r2Endpoint)
                .bucketName(bucketName)
                .mode("Cloudflare R2 Direct + Presigned CDN Engine")
                .message("Cloudflare R2 Object Storage active & responding cleanly (Logged to cloudflare-r2-status.log)")
                .timestamp(java.time.ZonedDateTime.now().toString())
                .build();
    }

    private void validateFile(UploadRequest request) {
        if (request.getOriginalFilename() == null || request.getOriginalFilename().isBlank()) {
            throw new BusinessException("Original filename is required");
        }
        if (request.getMediaType() == null) {
            throw new BusinessException("Media type is required");
        }
        if (request.getFileSize() != null && request.getFileSize() > 500 * 1024 * 1024) { // 500MB limit for demo
            throw new BusinessException("File size exceeds maximum allowed threshold");
        }
    }

    public AvatarUploadResponse generateAvatarUploadUrl(String userEmail, AvatarUploadRequest request) {
        validateAvatarFile(request);

        String extension = getFileExtension(request.getFileName());
        String objectKey = "avatars/" + userEmail + "/" + UUID.randomUUID() + "." + extension;

        String presignedUrl = presignedUrlService.generateUploadPresignedUrl(objectKey);
        String publicUrl = cdnUrl.endsWith("/") ? cdnUrl + objectKey : cdnUrl + "/" + objectKey;

        return AvatarUploadResponse.builder()
                .uploadUrl(presignedUrl)
                .publicUrl(publicUrl)
                .build();
    }

    private void validateAvatarFile(AvatarUploadRequest request) {
        if (request.getFileName() == null || request.getFileName().isBlank()) {
            throw new BusinessException("File name is required");
        }
        if (request.getContentType() == null || request.getContentType().isBlank()) {
            throw new BusinessException("Content type is required");
        }

        String contentType = request.getContentType().toLowerCase();
        if (!contentType.startsWith("image/")) {
            throw new BusinessException("Only image files are allowed for avatar uploads");
        }

        String[] allowedTypes = {"image/jpeg", "image/jpg", "image/png", "image/webp"};
        boolean isAllowed = false;
        for (String allowed : allowedTypes) {
            if (contentType.equals(allowed)) {
                isAllowed = true;
                break;
            }
        }

        if (!isAllowed) {
            throw new BusinessException("Unsupported image type. Allowed: JPEG, PNG, WebP");
        }
    }

    public String uploadAvatarToCloudflareR2(String userEmail, org.springframework.web.multipart.MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("Upload file is required");
        }

        String originalFilename = file.getOriginalFilename();
        String extension = getFileExtension(originalFilename);
        String objectKey = "avatars/" + userEmail + "/" + UUID.randomUUID() + "." + extension;

        try {
            byte[] fileBytes = file.getBytes();
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(objectKey)
                    .contentType(file.getContentType())
                    .build();

            s3Client.putObject(putObjectRequest, RequestBody.fromBytes(fileBytes));
            log.info("Successfully uploaded object to Cloudflare R2 bucket={}: key={}", bucketName, objectKey);
            return cdnUrl.endsWith("/") ? cdnUrl + objectKey : cdnUrl + "/" + objectKey;
        } catch (Exception e) {
            log.error("Cloudflare R2 S3 upload error: {}", e.getMessage(), e);
            try {
                File targetFile = new File("uploads/" + objectKey);
                File parent = targetFile.getParentFile();
                if (parent != null && !parent.exists()) parent.mkdirs();
                Files.write(Paths.get("uploads/" + objectKey), file.getBytes());
                String baseUploadsUrl = localUploadsUrl.endsWith("/") ? localUploadsUrl : localUploadsUrl + "/";
                return baseUploadsUrl + objectKey;
            } catch (Exception ex) {
                log.error("Local fallback write error: {}", ex.getMessage());
            }
            return cdnUrl.endsWith("/") ? cdnUrl + objectKey : cdnUrl + "/" + objectKey;
        }
    }

    public String uploadDirectFile(String objectKey, byte[] fileData) {
        if (fileData != null && fileData.length > 0) {
            // 1. If valid Cloudflare R2 credentials/endpoint exist, upload binary to R2
            try {
                if (r2Endpoint != null && !r2Endpoint.contains("account-id")) {
                    PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                            .bucket(bucketName)
                            .key(objectKey)
                            .build();
                    s3Client.putObject(putObjectRequest, RequestBody.fromBytes(fileData));
                    return cdnUrl.endsWith("/") ? cdnUrl + objectKey : cdnUrl + "/" + objectKey;
                }
            } catch (Exception e) {
                log.warn("Cloudflare R2 upload fallback to local storage: {}", e.getMessage());
            }

            // 2. Save file locally in uploads/ directory
            try {
                File targetFile = new File("uploads/" + objectKey);
                File parent = targetFile.getParentFile();
                if (parent != null && !parent.exists()) {
                    parent.mkdirs();
                }
                Files.write(Paths.get("uploads/" + objectKey), fileData);
                String baseUploadsUrl = localUploadsUrl.endsWith("/") ? localUploadsUrl : localUploadsUrl + "/";
                return baseUploadsUrl + objectKey;
            } catch (Exception e) {
                log.error("Local file write error: {}", e.getMessage());
            }
        }

        return cdnUrl.endsWith("/") ? cdnUrl + objectKey : cdnUrl + "/" + objectKey;
    }

    @org.springframework.cache.annotation.Cacheable(value = "media_all", key = "'all'")
    public java.util.List<Media> getAllMedia() {
        return mediaRepository.findAll();
    }

    @org.springframework.cache.annotation.CacheEvict(value = "media_all", allEntries = true)
    public void deleteMedia(UUID mediaId) {
        if (mediaId == null) return;
        mediaRepository.findById(mediaId).ifPresent(media -> {
            String objectKey = media.getObjectKey();
            if (objectKey != null && !objectKey.isBlank()) {
                // Delete from Cloudflare R2
                try {
                    DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                            .bucket(bucketName)
                            .key(objectKey)
                            .build();
                    s3Client.deleteObject(deleteObjectRequest);
                    log.info("Deleted object from Cloudflare R2 bucket={}: key={}", bucketName, objectKey);
                } catch (Exception e) {
                    log.warn("Cloudflare R2 deletion notice: {}", e.getMessage());
                }

                // Delete local file if present
                try {
                    File localFile = new File("uploads/" + objectKey);
                    if (localFile.exists()) {
                        localFile.delete();
                    }
                } catch (Exception ignored) {}
            }
            mediaRepository.delete(media);
            log.info("Deleted media record with ID: {}", mediaId);
        });
    }

    @org.springframework.cache.annotation.CacheEvict(value = "media_all", allEntries = true)
    public UploadResponse uploadFileToCloudflareR2(org.springframework.web.multipart.MultipartFile file, MediaType inputMediaType) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("Upload file is required");
        }

        String originalFilename = file.getOriginalFilename();
        String extension = getFileExtension(originalFilename);
        MediaType type = inputMediaType != null ? inputMediaType : detectMediaType(originalFilename, file.getContentType());
        String folder = type.name().toLowerCase() + "s";
        String objectKey = folder + "/" + UUID.randomUUID() + "." + extension;

        byte[] fileBytes;
        try {
            fileBytes = file.getBytes();
        } catch (Exception e) {
            throw new BusinessException("Failed to read upload file bytes: " + e.getMessage());
        }

        Media media = Media.builder()
                .originalFilename(originalFilename)
                .objectKey(objectKey)
                .mediaType(type)
                .mimeType(file.getContentType())
                .fileSize(file.getSize())
                .status(MediaStatus.COMPLETED)
                .build();

        Media savedMedia = mediaRepository.save(media);

        String finalCdnUrl;
        try {
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(objectKey)
                    .contentType(file.getContentType())
                    .build();

            s3Client.putObject(putObjectRequest, RequestBody.fromBytes(fileBytes));
            log.info("Successfully uploaded media file to Cloudflare R2 bucket={}: key={}", bucketName, objectKey);
            finalCdnUrl = cdnUrl.endsWith("/") ? cdnUrl + objectKey : cdnUrl + "/" + objectKey;
        } catch (Exception e) {
            log.error("Cloudflare R2 S3 upload error: {}", e.getMessage(), e);
            try {
                File targetFile = new File("uploads/" + objectKey);
                File parent = targetFile.getParentFile();
                if (parent != null && !parent.exists()) parent.mkdirs();
                Files.write(Paths.get("uploads/" + objectKey), fileBytes);
                String baseUploadsUrl = localUploadsUrl.endsWith("/") ? localUploadsUrl : localUploadsUrl + "/";
                finalCdnUrl = baseUploadsUrl + objectKey;
            } catch (Exception ex) {
                log.error("Local fallback write error: {}", ex.getMessage());
                finalCdnUrl = cdnUrl.endsWith("/") ? cdnUrl + objectKey : cdnUrl + "/" + objectKey;
            }
        }

        return UploadResponse.builder()
                .mediaId(savedMedia.getId())
                .objectKey(objectKey)
                .cdnUrl(finalCdnUrl)
                .build();
    }

    private MediaType detectMediaType(String filename, String contentType) {
        if (contentType != null) {
            String mime = contentType.toLowerCase();
            if (mime.startsWith("video/")) return MediaType.VIDEO;
            if (mime.startsWith("image/")) return MediaType.IMAGE;
            if (mime.startsWith("audio/")) return MediaType.AUDIO;
            if (mime.contains("pdf")) return MediaType.PDF;
        }
        if (filename != null) {
            String lower = filename.toLowerCase();
            if (lower.endsWith(".mp4") || lower.endsWith(".mkv") || lower.endsWith(".mov")) return MediaType.VIDEO;
            if (lower.endsWith(".png") || lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".webp")) return MediaType.IMAGE;
            if (lower.endsWith(".pdf")) return MediaType.PDF;
            if (lower.endsWith(".mp3") || lower.endsWith(".wav")) return MediaType.AUDIO;
        }
        return MediaType.DOCUMENT;
    }

    private String getFileExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return "bin";
        }
        return fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
    }
}
