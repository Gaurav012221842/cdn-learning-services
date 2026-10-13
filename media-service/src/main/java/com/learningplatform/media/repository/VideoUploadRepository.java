package com.learningplatform.media.repository;

import com.learningplatform.media.entity.VideoUpload;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface VideoUploadRepository extends JpaRepository<VideoUpload, UUID> {
    Optional<VideoUpload> findByUploadId(String uploadId);
    Optional<VideoUpload> findByObjectKeyAndStatus(String objectKey, VideoUpload.VideoUploadStatus status);
}
