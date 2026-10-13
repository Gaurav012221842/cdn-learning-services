package com.learningplatform.common.events;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Emitted when video asset is uploaded to Cloudflare R2 and needs HLS transcoding.
 * Topic: media-transcoding-events
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MediaTranscodingEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    private UUID mediaId;
    private String objectKey;
    private List<String> targetQualities;
    private Instant timestamp;
}
