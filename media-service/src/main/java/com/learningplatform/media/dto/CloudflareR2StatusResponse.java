package com.learningplatform.media.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CloudflareR2StatusResponse {
    private boolean connected;
    private String endpoint;
    private String bucketName;
    private String mode;
    private String message;
    private String timestamp;
}
