package com.learningplatform.media.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InitiateMultipartRequest {
    private String fileName;
    private Long fileSize;
    private Long chunkSize; // optional, default 10MB
    private String mimeType;
}
