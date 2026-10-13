package com.learningplatform.payment.coupon;

import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CouponService {

    private final CouponRepository couponRepository;

    @Cacheable(value = "coupon_codes", key = "#code")
    public Coupon validateCoupon(String code) {
        Coupon coupon = couponRepository.findByCode(code)
                .orElseThrow(() -> new RuntimeException("Invalid coupon code"));

        if (coupon.getUsedCount() >= coupon.getMaxUses()) {
            throw new RuntimeException("Coupon usage limit reached");
        }
        return coupon;
    }

    @CacheEvict(value = "coupon_codes", allEntries = true)
    public Coupon createCoupon(String code, BigDecimal discountAmount) {
        Coupon coupon = Coupon.builder()
                .code(code.toUpperCase())
                .discountAmount(discountAmount)
                .build();
        return couponRepository.save(coupon);
    }
}
