package com.learningplatform.media.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.ZonedDateTime;
import java.util.UUID;

@Entity
@Table(name = "video_uploads")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VideoUpload {

    @Id
    private UUID id;

    @Column(name = "media_id")
    private UUID mediaId;

    @Column(name = "upload_id", columnDefinition = "TEXT", nullable = false)
    private String uploadId;

    @Column(name = "object_key", columnDefinition = "TEXT", nullable = false)
    private String objectKey;

    @Column(name = "file_name", columnDefinition = "TEXT", nullable = false)
    private String fileName;

    @Column(name = "file_size", nullable = false)
    private Long fileSize;

    @Column(name = "chunk_size", nullable = false)
    private Long chunkSize;

    @Column(name = "total_parts", nullable = false)
    private Integer totalParts;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VideoUploadStatus status;

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "created_at")
    private ZonedDateTime createdAt;

    @Column(name = "updated_at")
    private ZonedDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        if (id == null) id = UUID.randomUUID();
        if (status == null) status = VideoUploadStatus.INITIATED;
        if (createdAt == null) createdAt = ZonedDateTime.now();
        if (updatedAt == null) updatedAt = ZonedDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = ZonedDateTime.now();
    }

    public enum VideoUploadStatus {
        INITIATED,
        UPLOADING,
        COMPLETED,
        FAILED,
        ABORTED
    }
}
