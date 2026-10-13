package com.learningplatform.payment;

import com.learningplatform.payment.producer.PaymentKafkaProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final RazorpayService razorpayService;
    private final PaymentKafkaProducer paymentKafkaProducer;

    public Payment initiatePayment(UUID userId, String userEmail, UUID courseId, BigDecimal clientAmount) {
        BigDecimal authoritativePrice = (clientAmount != null && clientAmount.compareTo(BigDecimal.ZERO) > 0)
                ? clientAmount
                : new BigDecimal("49.99");

        String orderId = razorpayService.createOrder(authoritativePrice, "INR");
        Payment payment = Payment.builder()
                .userId(userId != null ? userId : UUID.randomUUID())
                .courseId(courseId)
                .amount(authoritativePrice)
                .razorpayOrderId(orderId)
                .status(PaymentStatus.PENDING)
                .build();
        return paymentRepository.save(payment);
    }

    public Payment completePayment(String razorpayOrderId, String razorpayPaymentId, String signature,
                                  UUID userId, String userEmail, UUID courseId, BigDecimal amount) {
        // 🔒 Order Integrity Check: Must match a pre-registered server order
        Payment payment = paymentRepository.findByRazorpayOrderId(razorpayOrderId)
                .orElseThrow(() -> new IllegalArgumentException("Unrecognized Razorpay order. Order was not initiated on this server."));

        if (razorpayService.verifySignature(razorpayOrderId, razorpayPaymentId, signature)) {
            payment.setRazorpayPaymentId(razorpayPaymentId);
            payment.setStatus(PaymentStatus.COMPLETED);
            Payment savedPayment = paymentRepository.save(payment);

            // 🚀 Event-Driven Saga: Emit Kafka event to provision course enrollment asynchronously
            paymentKafkaProducer.publishPaymentCompleted(
                    savedPayment.getId(),
                    razorpayPaymentId,
                    razorpayOrderId,
                    savedPayment.getUserId(),
                    userEmail,
                    savedPayment.getCourseId(),
                    savedPayment.getAmount()
            );
        } else {
            payment.setStatus(PaymentStatus.FAILED);
            paymentRepository.save(payment);
            throw new SecurityException("Payment signature verification failed. Tampered or fraudulent transaction.");
        }
        return payment;
    }

    public List<Payment> getUserPayments(UUID userId) {
        if (userId == null) return List.of();
        return paymentRepository.findByUserId(userId);
    }
}
