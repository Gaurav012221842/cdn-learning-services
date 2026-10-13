package com.learningplatform.course.pricing;

import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PricingService {

    private final PricingRepository pricingRepository;

    @Cacheable(value = "course_pricing", key = "#courseId")
    public CoursePricing getPricingByCourseId(UUID courseId) {
        return pricingRepository.findByCourseId(courseId)
                .orElseGet(() -> CoursePricing.builder()
                        .courseId(courseId)
                        .basePrice(BigDecimal.ZERO)
                        .currency("USD")
                        .build());
    }

    @CacheEvict(value = "course_pricing", key = "#courseId")
    public CoursePricing setCoursePricing(UUID courseId, BigDecimal basePrice, BigDecimal discountPrice) {
        CoursePricing pricing = pricingRepository.findByCourseId(courseId)
                .orElse(CoursePricing.builder().courseId(courseId).build());

        pricing.setBasePrice(basePrice);
        pricing.setDiscountPrice(discountPrice);
        return pricingRepository.save(pricing);
    }
}
