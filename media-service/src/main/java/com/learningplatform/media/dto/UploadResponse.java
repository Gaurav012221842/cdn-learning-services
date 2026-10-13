package com.learningplatform.media.dto;

import lombok.Builder;
import lombok.Data;
import java.util.UUID;

@Data
@Builder
public class UploadResponse {
    private UUID mediaId;
    private String objectKey;
    private String presignedUploadUrl;
    private String cdnUrl;
}
