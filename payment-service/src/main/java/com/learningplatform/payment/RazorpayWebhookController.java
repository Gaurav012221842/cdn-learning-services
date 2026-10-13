package com.learningplatform.payment;

import com.learningplatform.common.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/payments/webhook")
@RequiredArgsConstructor
public class RazorpayWebhookController {

    @PostMapping
    public ResponseEntity<ApiResponse<String>> handleWebhook(@RequestBody String payload, @RequestHeader("X-Razorpay-Signature") String signature) {
        return ResponseEntity.ok(ApiResponse.success("Webhook received", "SUCCESS"));
    }
}
