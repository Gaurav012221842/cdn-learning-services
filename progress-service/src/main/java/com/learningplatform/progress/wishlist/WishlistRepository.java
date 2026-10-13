package com.learningplatform.progress.wishlist;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface WishlistRepository extends JpaRepository<Wishlist, UUID> {
    List<Wishlist> findByStudentId(UUID studentId);
    Optional<Wishlist> findByStudentIdAndCourseId(UUID studentId, UUID courseId);
    void deleteByStudentIdAndCourseId(UUID studentId, UUID courseId);
}
