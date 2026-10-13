package com.learningplatform.enrollment.consumer;

import com.learningplatform.common.events.PaymentCompletedEvent;
import com.learningplatform.enrollment.EnrollmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Event-Driven Saga Consumer: Listens for completed payment events and auto-provisions course access.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentEventsConsumer {

    private final EnrollmentService enrollmentService;

    @KafkaListener(topics = "payment-completed-events", groupId = "enrollment-service-group")
    public void handlePaymentCompleted(PaymentCompletedEvent event) {
        if (event == null || event.getCourseId() == null) {
            log.warn("Received invalid PaymentCompletedEvent");
            return;
        }

        log.info("Received PaymentCompletedEvent from Kafka: userId={}, courseId={}, paymentId={}",
                event.getUserId(), event.getCourseId(), event.getPaymentId());

        try {
            enrollmentService.enrollStudent(event.getUserId(), event.getUserEmail(), event.getCourseId());
            log.info("Auto-provisioned enrollment for user {} in course {}", event.getUserId(), event.getCourseId());
        } catch (Exception e) {
            log.error("Failed to auto-provision enrollment for user {}: {}", event.getUserId(), e.getMessage());
        }
    }
}
