package com.learningplatform.progress.review;

import com.learningplatform.progress.review.dto.CourseReviewDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReviewService {

    private final ReviewRepository reviewRepository;

    @Cacheable(value = "course_reviews", key = "#courseId")
    public List<CourseReviewDTO> getCourseReviews(UUID courseId) {
        List<CourseReview> reviews = reviewRepository.findByCourseId(courseId);
        return reviews.stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Cacheable(value = "course_rating_summary", key = "#courseId")
    public Map<String, Object> getCourseRatingSummary(UUID courseId) {
        List<CourseReview> reviews = reviewRepository.findByCourseId(courseId);
        double averageRating = reviews.stream()
                .mapToInt(r -> r.getRating() != null ? r.getRating() : 5)
                .average()
                .orElse(5.0);

        long fiveStars = reviews.stream().filter(r -> r.getRating() != null && r.getRating() == 5).count();
        long fourStars = reviews.stream().filter(r -> r.getRating() != null && r.getRating() == 4).count();
        long threeStars = reviews.stream().filter(r -> r.getRating() != null && r.getRating() == 3).count();
        long twoStars = reviews.stream().filter(r -> r.getRating() != null && r.getRating() == 2).count();
        long oneStar = reviews.stream().filter(r -> r.getRating() != null && r.getRating() == 1).count();

        Map<String, Object> summary = new HashMap<>();
        summary.put("averageRating", Math.round(averageRating * 10.0) / 10.0);
        summary.put("totalReviews", reviews.size());
        summary.put("starBreakdown", Map.of(
                "5", fiveStars,
                "4", fourStars,
                "3", threeStars,
                "2", twoStars,
                "1", oneStar
        ));
        return summary;
    }

    @CacheEvict(value = {"course_reviews", "course_rating_summary"}, allEntries = true)
    public CourseReviewDTO addReview(UUID studentId, String email, UUID courseId, Integer rating, String comment) {
        UUID validStudentId = resolveStudentId(studentId, email);

        CourseReview review = CourseReview.builder()
                .id(UUID.randomUUID())
                .studentId(validStudentId)
                .courseId(courseId)
                .rating(rating != null && rating >= 1 && rating <= 5 ? rating : 5)
                .comment(comment != null ? comment.trim() : "")
                .createdAt(ZonedDateTime.now())
                .build();

        CourseReview saved = reviewRepository.save(review);
        log.info("Student {} submitted review for course {} with rating {}", validStudentId, courseId, rating);
        return toDTO(saved);
    }

    @CacheEvict(value = {"course_reviews", "course_rating_summary"}, allEntries = true)
    public void deleteReview(UUID reviewId) {
        reviewRepository.deleteById(reviewId);
    }

    private CourseReviewDTO toDTO(CourseReview r) {
        String shortId = r.getStudentId() != null ? r.getStudentId().toString().substring(0, 8) : "student";
        return CourseReviewDTO.builder()
                .id(r.getId())
                .studentId(r.getStudentId())
                .studentName("Verified Student (" + shortId + ")")
                .studentEmail("student@" + shortId + ".com")
                .avatarUrl(null)
                .courseId(r.getCourseId())
                .rating(r.getRating() != null ? r.getRating() : 5)
                .comment(r.getComment())
                .createdAt(r.getCreatedAt())
                .build();
    }

    private UUID resolveStudentId(UUID studentId, String email) {
        if (studentId != null) return studentId;
        if (email != null && !email.isBlank()) {
            return UUID.nameUUIDFromBytes(email.trim().toLowerCase().getBytes());
        }
        return UUID.randomUUID();
    }
}
