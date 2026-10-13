package com.learningplatform.progress.wishlist;

import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WishlistService {

    private final WishlistRepository wishlistRepository;

    @Cacheable(value = "user_wishlist", key = "(#studentId != null ? #studentId.toString() : '') + '_' + (#email != null ? #email : '')")
    public List<Wishlist> getUserWishlist(UUID studentId, String email) {
        UUID validId = resolveStudentId(studentId, email);
        return wishlistRepository.findByStudentId(validId);
    }

    @CacheEvict(value = {"user_wishlist", "wishlist_check"}, allEntries = true)
    public Wishlist addToWishlist(UUID studentId, String email, UUID courseId) {
        UUID validId = resolveStudentId(studentId, email);
        return wishlistRepository.findByStudentIdAndCourseId(validId, courseId)
                .orElseGet(() -> wishlistRepository.save(Wishlist.builder()
                        .studentId(validId)
                        .courseId(courseId)
                        .build()));
    }

    @Transactional
    @CacheEvict(value = {"user_wishlist", "wishlist_check"}, allEntries = true)
    public void removeFromWishlist(UUID studentId, String email, UUID courseId) {
        UUID validId = resolveStudentId(studentId, email);
        wishlistRepository.deleteByStudentIdAndCourseId(validId, courseId);
    }

    @Cacheable(value = "wishlist_check", key = "(#studentId != null ? #studentId.toString() : '') + '_' + (#email != null ? #email : '') + '_' + #courseId.toString()")
    public boolean isInWishlist(UUID studentId, String email, UUID courseId) {
        UUID validId = resolveStudentId(studentId, email);
        return wishlistRepository.findByStudentIdAndCourseId(validId, courseId).isPresent();
    }

    private UUID resolveStudentId(UUID studentId, String email) {
        if (studentId != null) return studentId;
        if (email != null && !email.isBlank()) {
            return UUID.nameUUIDFromBytes(email.trim().toLowerCase().getBytes());
        }
        return UUID.randomUUID();
    }
}
