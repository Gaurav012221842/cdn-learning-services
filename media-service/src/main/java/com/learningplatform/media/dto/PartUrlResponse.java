package com.learningplatform.media.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PartUrlResponse {
    private Integer partNumber;
    private String presignedUrl;
    private Long expiresInSeconds;
}
