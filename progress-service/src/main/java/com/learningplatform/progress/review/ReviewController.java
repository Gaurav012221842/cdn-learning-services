package com.learningplatform.progress.review;

import com.learningplatform.common.dto.ApiResponse;
import com.learningplatform.progress.review.dto.CourseReviewDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @GetMapping("/course/{courseId}")
    public ResponseEntity<ApiResponse<List<CourseReviewDTO>>> getReviews(@PathVariable UUID courseId) {
        return ResponseEntity.ok(ApiResponse.success("Reviews retrieved", reviewService.getCourseReviews(courseId)));
    }

    @GetMapping("/course/{courseId}/summary")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getRatingSummary(@PathVariable UUID courseId) {
        return ResponseEntity.ok(ApiResponse.success("Rating summary retrieved", reviewService.getCourseRatingSummary(courseId)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<CourseReviewDTO>> addReview(
            @RequestParam(required = false) String studentId,
            @RequestParam(required = false) String email,
            @RequestParam UUID courseId,
            @RequestParam(defaultValue = "5") Integer rating,
            @RequestParam(required = false) String comment
    ) {
        UUID uid = parseUUID(studentId);
        return ResponseEntity.ok(ApiResponse.success("Review submitted successfully", reviewService.addReview(uid, email, courseId, rating, comment)));
    }

    @DeleteMapping("/{reviewId}")
    public ResponseEntity<ApiResponse<String>> deleteReview(@PathVariable UUID reviewId) {
        reviewService.deleteReview(reviewId);
        return ResponseEntity.ok(ApiResponse.success("Review deleted successfully", "SUCCESS"));
    }

    private UUID parseUUID(String str) {
        if (str == null || str.isBlank() || str.equals("undefined") || str.equals("null") || str.equals("current")) {
            return null;
        }
        try {
            return UUID.fromString(str);
        } catch (Exception e) {
            return null;
        }
    }
}
