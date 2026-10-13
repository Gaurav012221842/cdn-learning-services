package com.learningplatform.common.events;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Emitted when a Razorpay payment succeeds.
 * Consumed by Enrollment Service, Progress Service, and Notification Service.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentCompletedEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    private UUID paymentId;
    private String razorpayPaymentId;
    private String razorpayOrderId;
    private UUID userId;
    private String userEmail;
    private UUID courseId;
    private BigDecimal amount;
    private String currency;
    private Instant completedAt;
}
