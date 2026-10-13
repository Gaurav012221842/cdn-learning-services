package com.learningplatform.progress.wishlist;

import com.learningplatform.common.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/wishlists")
@RequiredArgsConstructor
public class WishlistController {

    private final WishlistService wishlistService;

    @GetMapping("/student/{studentId}")
    public ResponseEntity<ApiResponse<List<Wishlist>>> getWishlist(
            @PathVariable(required = false) String studentId,
            @RequestParam(required = false) String email
    ) {
        UUID uid = parseUUID(studentId);
        return ResponseEntity.ok(ApiResponse.success("Wishlist retrieved", wishlistService.getUserWishlist(uid, email)));
    }

    @GetMapping("/check")
    public ResponseEntity<ApiResponse<Map<String, Boolean>>> checkWishlist(
            @RequestParam(required = false) String studentId,
            @RequestParam(required = false) String email,
            @RequestParam UUID courseId
    ) {
        UUID uid = parseUUID(studentId);
        boolean inWishlist = wishlistService.isInWishlist(uid, email, courseId);
        return ResponseEntity.ok(ApiResponse.success("Wishlist check completed", Map.of("inWishlist", inWishlist)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Wishlist>> addToWishlist(
            @RequestParam(required = false) String studentId,
            @RequestParam(required = false) String email,
            @RequestParam UUID courseId
    ) {
        UUID uid = parseUUID(studentId);
        return ResponseEntity.ok(ApiResponse.success("Added to wishlist", wishlistService.addToWishlist(uid, email, courseId)));
    }

    @DeleteMapping
    public ResponseEntity<ApiResponse<String>> removeFromWishlist(
            @RequestParam(required = false) String studentId,
            @RequestParam(required = false) String email,
            @RequestParam UUID courseId
    ) {
        UUID uid = parseUUID(studentId);
        wishlistService.removeFromWishlist(uid, email, courseId);
        return ResponseEntity.ok(ApiResponse.success("Removed from wishlist", "SUCCESS"));
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
