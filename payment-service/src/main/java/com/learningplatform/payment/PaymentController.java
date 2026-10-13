package com.learningplatform.payment;

import com.learningplatform.common.constants.SecurityHeaders;
import com.learningplatform.common.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/initiate")
    public ResponseEntity<ApiResponse<Payment>> initiatePayment(
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) String userEmail,
            @RequestParam UUID courseId,
            @RequestParam(required = false) BigDecimal amount,
            @RequestHeader(value = SecurityHeaders.USER_ID_HEADER, required = false) String headerUserId,
            @RequestHeader(value = SecurityHeaders.USER_EMAIL_HEADER, required = false) String headerUserEmail
    ) {
        UUID uid = parseUUID(userId != null ? userId : headerUserId);
        String email = userEmail != null ? userEmail : headerUserEmail;
        return ResponseEntity.ok(ApiResponse.success("Payment order initiated", paymentService.initiatePayment(uid, email, courseId, amount)));
    }

    @PostMapping("/verify")
    public ResponseEntity<ApiResponse<Payment>> verifyPayment(
            @RequestParam String razorpayOrderId,
            @RequestParam String razorpayPaymentId,
            @RequestParam String signature,
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) String userEmail,
            @RequestParam(required = false) UUID courseId,
            @RequestParam(required = false) BigDecimal amount,
            @RequestHeader(value = SecurityHeaders.USER_ID_HEADER, required = false) String headerUserId,
            @RequestHeader(value = SecurityHeaders.USER_EMAIL_HEADER, required = false) String headerUserEmail
    ) {
        UUID uid = parseUUID(userId != null ? userId : headerUserId);
        String email = userEmail != null ? userEmail : headerUserEmail;
        return ResponseEntity.ok(ApiResponse.success("Payment verified", paymentService.completePayment(razorpayOrderId, razorpayPaymentId, signature, uid, email, courseId, amount)));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<ApiResponse<List<Payment>>> getUserPayments(
            @PathVariable String userId
    ) {
        UUID uid = parseUUID(userId);
        return ResponseEntity.ok(ApiResponse.success("Payments retrieved", paymentService.getUserPayments(uid)));
    }

    private UUID parseUUID(String str) {
        if (str == null || str.isBlank() || str.equals("undefined") || str.equals("null")) return null;
        try {
            return UUID.fromString(str);
        } catch (Exception e) {
            return null;
        }
    }
}
