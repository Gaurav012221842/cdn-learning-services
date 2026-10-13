package com.learningplatform.payment.coupon;

import com.learningplatform.common.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/v1/coupons")
@RequiredArgsConstructor
public class CouponController {

    private final CouponService couponService;

    @GetMapping("/validate")
    public ResponseEntity<ApiResponse<Coupon>> validateCoupon(@RequestParam String code) {
        return ResponseEntity.ok(ApiResponse.success("Coupon is valid", couponService.validateCoupon(code)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Coupon>> createCoupon(@RequestParam String code, @RequestParam BigDecimal discountAmount) {
        return ResponseEntity.ok(ApiResponse.success("Coupon created", couponService.createCoupon(code, discountAmount)));
    }
}
