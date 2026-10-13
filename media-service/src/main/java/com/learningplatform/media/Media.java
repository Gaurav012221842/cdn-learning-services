package com.learningplatform.media;

import jakarta.persistence.*;
import lombok.*;

import java.time.ZonedDateTime;
import java.util.UUID;

@Entity
@Table(name = "media")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Media {

    @Id
    private UUID id;

    @Column(name = "original_filename", columnDefinition = "TEXT", nullable = false)
    private String originalFilename;

    @Column(name = "object_key", columnDefinition = "TEXT", nullable = false)
    private String objectKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "media_type", nullable = false)
    private MediaType mediaType;

    @Column(name = "mime_type")
    private String mimeType;

    @Column(name = "file_size")
    private Long fileSize;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MediaStatus status;

    @Column(name = "uploaded_by")
    private UUID uploadedBy;

    @Column(name = "created_at")
    private ZonedDateTime createdAt;

    @Column(name = "updated_at")
    private ZonedDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        if (id == null) id = UUID.randomUUID();
        if (status == null) status = MediaStatus.UPLOADING;
        if (createdAt == null) createdAt = ZonedDateTime.now();
        if (updatedAt == null) updatedAt = ZonedDateTime.now();
    }
}
