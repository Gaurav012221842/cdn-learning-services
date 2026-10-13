package com.learningplatform.media.producer;

import com.learningplatform.common.events.MediaTranscodingEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class MediaKafkaProducer {

    public static final String MEDIA_TRANSCODING_TOPIC = "media-transcoding-events";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publishTranscodingEvent(UUID mediaId, String objectKey) {
        MediaTranscodingEvent event = MediaTranscodingEvent.builder()
                .mediaId(mediaId)
                .objectKey(objectKey)
                .targetQualities(List.of("1080p", "720p", "480p"))
                .timestamp(Instant.now())
                .build();

        try {
            log.info("Emitting MediaTranscodingEvent to Kafka topic [{}]: mediaId={}", MEDIA_TRANSCODING_TOPIC, mediaId);
            kafkaTemplate.send(MEDIA_TRANSCODING_TOPIC, mediaId.toString(), event);
        } catch (Exception e) {
            log.warn("Failed to publish MediaTranscodingEvent to Kafka (Kafka may be offline): {}", e.getMessage());
        }
    }
}
