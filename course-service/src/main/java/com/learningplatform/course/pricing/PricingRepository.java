package com.learningplatform.course.pricing;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PricingRepository extends JpaRepository<CoursePricing, UUID> {
    Optional<CoursePricing> findByCourseId(UUID courseId);
}
