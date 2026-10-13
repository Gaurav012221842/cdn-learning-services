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
public class UploadedPartsResponse {
    private UUID uploadSessionId;
    private String uploadId;
    private String objectKey;
    private List<InitiateMultipartResponse.UploadedPartSummary> parts;
}
