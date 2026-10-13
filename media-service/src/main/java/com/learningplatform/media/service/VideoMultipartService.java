package com.learningplatform.media.service;

import com.learningplatform.common.exception.BusinessException;
import com.learningplatform.media.Media;
import com.learningplatform.media.MediaRepository;
import com.learningplatform.media.MediaStatus;
import com.learningplatform.media.MediaType;
import com.learningplatform.media.dto.*;
import com.learningplatform.media.entity.VideoUpload;
import com.learningplatform.media.repository.VideoUploadRepository;
import com.learningplatform.media.storage.ObjectKeyGenerator;
import com.learningplatform.media.storage.R2MultipartService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class VideoMultipartService {

    private static final long DEFAULT_CHUNK_SIZE = 10 * 1024 * 1024L; // 10 MB default chunk size
    private static final long MIN_CHUNK_SIZE = 5 * 1024 * 1024L;      // 5 MB S3 minimum part size

    private final VideoUploadRepository videoUploadRepository;
    private final MediaRepository mediaRepository;
    private final R2MultipartService r2MultipartService;
    private final ObjectKeyGenerator objectKeyGenerator;

    @Autowired(required = false)
    private KafkaTemplate<String, String> kafkaTemplate;

    @Transactional
    public InitiateMultipartResponse initiateMultipart(InitiateMultipartRequest request, String userEmail) {
        if (request == null || request.getFileName() == null || request.getFileName().isBlank()) {
            throw new BusinessException("File name is required for video upload.");
        }
        if (request.getFileSize() == null || request.getFileSize() <= 0) {
            throw new BusinessException("Valid file size (in bytes) is required.");
        }

        long chunkSize = request.getChunkSize() != null && request.getChunkSize() >= MIN_CHUNK_SIZE
                ? request.getChunkSize()
                : DEFAULT_CHUNK_SIZE;

        int totalParts = (int) Math.ceil((double) request.getFileSize() / chunkSize);
        if (totalParts == 0) totalParts = 1;

        String objectKey = objectKeyGenerator.generateKey(request.getFileName(), "videos/raw");

        // 1. Create Media record in database
        Media media = Media.builder()
                .id(UUID.randomUUID())
                .originalFilename(request.getFileName())
                .objectKey(objectKey)
                .mediaType(MediaType.VIDEO)
                .mimeType(request.getMimeType() != null ? request.getMimeType() : "video/mp4")
                .fileSize(request.getFileSize())
                .status(MediaStatus.UPLOADING)
                .createdAt(ZonedDateTime.now())
                .build();
        media = mediaRepository.save(media);

        // 2. Initiate S3/R2 Multipart Upload
        String uploadId = r2MultipartService.createMultipartUpload(objectKey, media.getMimeType());

        // 3. Save VideoUpload tracking session
        VideoUpload videoUpload = VideoUpload.builder()
                .id(UUID.randomUUID())
                .mediaId(media.getId())
                .uploadId(uploadId)
                .objectKey(objectKey)
                .fileName(request.getFileName())
                .fileSize(request.getFileSize())
                .chunkSize(chunkSize)
                .totalParts(totalParts)
                .status(VideoUpload.VideoUploadStatus.INITIATED)
                .createdBy(userEmail != null ? userEmail : "admin")
                .createdAt(ZonedDateTime.now())
                .updatedAt(ZonedDateTime.now())
                .build();
        videoUpload = videoUploadRepository.save(videoUpload);

        List<InitiateMultipartResponse.UploadedPartSummary> alreadyUploadedParts =
                r2MultipartService.listUploadedParts(objectKey, uploadId);

        log.info("Initiated video upload session {} for file '{}' ({} bytes, {} parts)",
                videoUpload.getId(), request.getFileName(), request.getFileSize(), totalParts);

        return InitiateMultipartResponse.builder()
                .uploadSessionId(videoUpload.getId())
                .uploadId(uploadId)
                .mediaId(media.getId())
                .objectKey(objectKey)
                .chunkSize(chunkSize)
                .totalParts(totalParts)
                .alreadyUploadedParts(alreadyUploadedParts)
                .build();
    }

    public PartUrlResponse getPartUrl(PartUrlRequest request) {
        if (request == null || request.getUploadSessionId() == null || request.getPartNumber() == null) {
            throw new BusinessException("Upload session ID and part number are required.");
        }

        VideoUpload videoUpload = videoUploadRepository.findById(request.getUploadSessionId())
                .orElseThrow(() -> new BusinessException("Video upload session not found: " + request.getUploadSessionId()));

        if (videoUpload.getStatus() == VideoUpload.VideoUploadStatus.COMPLETED) {
            throw new BusinessException("Upload is already completed.");
        }
        if (videoUpload.getStatus() == VideoUpload.VideoUploadStatus.ABORTED) {
            throw new BusinessException("Upload session was aborted.");
        }

        if (videoUpload.getStatus() == VideoUpload.VideoUploadStatus.INITIATED) {
            videoUpload.setStatus(VideoUpload.VideoUploadStatus.UPLOADING);
            videoUploadRepository.save(videoUpload);
        }

        String presignedUrl = r2MultipartService.generatePresignedPartUrl(
                videoUpload.getObjectKey(),
                videoUpload.getUploadId(),
                request.getPartNumber()
        );

        return PartUrlResponse.builder()
                .partNumber(request.getPartNumber())
                .presignedUrl(presignedUrl)
                .expiresInSeconds(900L) // 15 minutes
                .build();
    }

    public String uploadPartChunk(UUID uploadSessionId, int partNumber, byte[] chunkData) {
        VideoUpload videoUpload = videoUploadRepository.findById(uploadSessionId)
                .orElseThrow(() -> new BusinessException("Video upload session not found: " + uploadSessionId));

        if (videoUpload.getStatus() == VideoUpload.VideoUploadStatus.INITIATED) {
            videoUpload.setStatus(VideoUpload.VideoUploadStatus.UPLOADING);
            videoUploadRepository.save(videoUpload);
        }

        return r2MultipartService.uploadPartDirect(
                videoUpload.getObjectKey(),
                videoUpload.getUploadId(),
                partNumber,
                chunkData
        );
    }

    public UploadedPartsResponse listUploadedParts(UUID uploadSessionId) {
        VideoUpload videoUpload = videoUploadRepository.findById(uploadSessionId)
                .orElseThrow(() -> new BusinessException("Video upload session not found: " + uploadSessionId));

        List<InitiateMultipartResponse.UploadedPartSummary> parts =
                r2MultipartService.listUploadedParts(videoUpload.getObjectKey(), videoUpload.getUploadId());

        return UploadedPartsResponse.builder()
                .uploadSessionId(videoUpload.getId())
                .uploadId(videoUpload.getUploadId())
                .objectKey(videoUpload.getObjectKey())
                .parts(parts)
                .build();
    }

    @Transactional
    public Media completeMultipartUpload(CompleteMultipartRequest request) {
        if (request == null || request.getUploadSessionId() == null || request.getParts() == null || request.getParts().isEmpty()) {
            throw new BusinessException("Upload session ID and list of completed parts are required.");
        }

        VideoUpload videoUpload = videoUploadRepository.findById(request.getUploadSessionId())
                .orElseThrow(() -> new BusinessException("Video upload session not found: " + request.getUploadSessionId()));

        // Complete Multipart in R2
        r2MultipartService.completeMultipartUpload(
                videoUpload.getObjectKey(),
                videoUpload.getUploadId(),
                request.getParts()
        );

        // Update database statuses
        videoUpload.setStatus(VideoUpload.VideoUploadStatus.COMPLETED);
        videoUpload.setUpdatedAt(ZonedDateTime.now());
        videoUploadRepository.save(videoUpload);

        Media media = mediaRepository.findById(videoUpload.getMediaId())
                .orElseThrow(() -> new BusinessException("Media not found for upload: " + videoUpload.getMediaId()));

        media.setStatus(MediaStatus.COMPLETED);
        media.setUpdatedAt(ZonedDateTime.now());
        Media savedMedia = mediaRepository.save(media);

        // Publish to Kafka for video-worker HLS transcoding
        if (kafkaTemplate != null) {
            try {
                log.info("Publishing video transcoding event to Kafka topic 'media-transcoding-events' for mediaId: {}", savedMedia.getId());
                kafkaTemplate.send("media-transcoding-events", savedMedia.getId().toString())
                        .whenComplete((result, ex) -> {
                            if (ex == null) {
                                log.info("✅ Kafka acknowledged event for mediaId: {} [partition: {}, offset: {}]",
                                        savedMedia.getId(), result.getRecordMetadata().partition(), result.getRecordMetadata().offset());
                            } else {
                                log.error("❌ Kafka failed delivering event for mediaId: {}: {}", savedMedia.getId(), ex.getMessage());
                            }
                        });
            } catch (Exception e) {
                log.warn("Could not dispatch Kafka transcoding event: {}", e.getMessage());
            }
        } else {
            log.warn("⚠️ KafkaTemplate is not configured or null.");
        }

        log.info("Successfully completed multipart upload for mediaId: {}", savedMedia.getId());
        return savedMedia;
    }

    @Transactional
    public void abortMultipartUpload(UUID uploadSessionId) {
        VideoUpload videoUpload = videoUploadRepository.findById(uploadSessionId)
                .orElseThrow(() -> new BusinessException("Video upload session not found: " + uploadSessionId));

        r2MultipartService.abortMultipartUpload(videoUpload.getObjectKey(), videoUpload.getUploadId());

        videoUpload.setStatus(VideoUpload.VideoUploadStatus.ABORTED);
        videoUpload.setUpdatedAt(ZonedDateTime.now());
        videoUploadRepository.save(videoUpload);

        if (videoUpload.getMediaId() != null) {
            mediaRepository.findById(videoUpload.getMediaId()).ifPresent(m -> {
                m.setStatus(MediaStatus.FAILED);
                mediaRepository.save(m);
            });
        }

        log.info("Aborted multipart video upload session {}", uploadSessionId);
    }
}
