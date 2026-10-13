package com.learningplatform.payment.producer;

import com.learningplatform.common.events.PaymentCompletedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentKafkaProducer {

    public static final String PAYMENT_COMPLETED_TOPIC = "payment-completed-events";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publishPaymentCompleted(UUID paymentId, String razorpayPaymentId, String razorpayOrderId,
                                       UUID userId, String userEmail, UUID courseId, BigDecimal amount) {
        PaymentCompletedEvent event = PaymentCompletedEvent.builder()
                .paymentId(paymentId)
                .razorpayPaymentId(razorpayPaymentId)
                .razorpayOrderId(razorpayOrderId)
                .userId(userId)
                .userEmail(userEmail)
                .courseId(courseId)
                .amount(amount)
                .currency("INR")
                .completedAt(Instant.now())
                .build();

        try {
            log.info("Emitting PaymentCompletedEvent to Kafka topic [{}]: paymentId={}, userId={}, courseId={}",
                    PAYMENT_COMPLETED_TOPIC, paymentId, userId, courseId);
            kafkaTemplate.send(PAYMENT_COMPLETED_TOPIC, userId != null ? userId.toString() : paymentId.toString(), event);
        } catch (Exception e) {
            log.warn("Could not dispatch PaymentCompletedEvent to Kafka (Kafka offline?): {}", e.getMessage());
        }
    }
}
