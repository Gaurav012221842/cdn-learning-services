package com.learningplatform.course.pricing;

import com.learningplatform.common.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/pricing")
@RequiredArgsConstructor
public class PricingController {

    private final PricingService pricingService;

    @GetMapping("/course/{courseId}")
    public ResponseEntity<ApiResponse<CoursePricing>> getPricing(@PathVariable UUID courseId) {
        return ResponseEntity.ok(ApiResponse.success("Pricing retrieved", pricingService.getPricingByCourseId(courseId)));
    }

    @PostMapping("/course/{courseId}")
    public ResponseEntity<ApiResponse<CoursePricing>> setPricing(
            @PathVariable UUID courseId,
            @RequestParam BigDecimal basePrice,
            @RequestParam(required = false) BigDecimal discountPrice
    ) {
        return ResponseEntity.ok(ApiResponse.success("Pricing updated", pricingService.setCoursePricing(courseId, basePrice, discountPrice)));
    }
}
