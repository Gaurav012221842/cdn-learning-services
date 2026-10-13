package com.learningplatform.user.producer;

import com.learningplatform.common.events.UserRegisteredEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class UserKafkaProducer {

    public static final String USER_REGISTERED_TOPIC = "user-registered-events";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publishUserRegistered(UUID userId, String email, String fullName, String role) {
        UserRegisteredEvent event = UserRegisteredEvent.builder()
                .userId(userId)
                .email(email)
                .fullName(fullName)
                .role(role)
                .registeredAt(Instant.now())
                .build();

        try {
            log.info("Emitting UserRegisteredEvent to Kafka topic [{}]: userId={}, email={}",
                    USER_REGISTERED_TOPIC, userId, email);
            kafkaTemplate.send(USER_REGISTERED_TOPIC, userId.toString(), event);
        } catch (Exception e) {
            log.warn("Could not dispatch UserRegisteredEvent to Kafka: {}", e.getMessage());
        }
    }
}
