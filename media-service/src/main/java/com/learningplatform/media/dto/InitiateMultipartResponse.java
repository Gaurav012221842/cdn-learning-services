package com.learningplatform.media.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InitiateMultipartResponse {
    private UUID uploadSessionId;
    private String uploadId;
    private UUID mediaId;
    private String objectKey;
    private Long chunkSize;
    private Integer totalParts;
    private List<UploadedPartSummary> alreadyUploadedParts;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UploadedPartSummary {
        private Integer partNumber;
        private String eTag;
        private Long size;
    }
}
