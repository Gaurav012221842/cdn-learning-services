package com.learningplatform.user.controller;

import com.learningplatform.common.dto.ApiResponse;
import com.learningplatform.user.dto.UpdateProfileRequest;
import com.learningplatform.user.dto.UserSessionDTO;
import com.learningplatform.user.entity.User;
import com.learningplatform.user.service.DeviceSessionService;
import com.learningplatform.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final DeviceSessionService deviceSessionService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<User>>> getAllUsers() {
        return ResponseEntity.ok(ApiResponse.success("Users retrieved", userService.getAllUsers()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<User>> getUserById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success("User retrieved", userService.getUserById(id)));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<User>> getCurrentUser(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            return ResponseEntity.status(401).body(ApiResponse.error("Unauthorized"));
        }
        return ResponseEntity.ok(ApiResponse.success("Current user profile retrieved", userService.getUserByEmail(authentication.getName())));
    }

    @GetMapping("/sessions/me")
    public ResponseEntity<ApiResponse<List<com.learningplatform.user.dto.UserSessionDTO>>> getMySessions(
            Authentication authentication,
            @RequestHeader(value = "Authorization", required = false) String authHeader
    ) {
        if (authentication == null || authentication.getName() == null) {
            return ResponseEntity.status(401).body(ApiResponse.error("Unauthorized"));
        }
        List<com.learningplatform.user.dto.UserSessionDTO> sessions =
                deviceSessionService.getUserSessions(authentication.getName(), authHeader);
        return ResponseEntity.ok(ApiResponse.success("Device sessions retrieved", sessions));
    }

    @GetMapping("/sessions/admin/all")
    public ResponseEntity<ApiResponse<List<com.learningplatform.user.dto.UserSessionDTO>>> getAllSessionsForAdmin(
            Authentication authentication
    ) {
        if (authentication == null || authentication.getName() == null) {
            return ResponseEntity.status(401).body(ApiResponse.error("Unauthorized"));
        }
        List<com.learningplatform.user.dto.UserSessionDTO> sessions =
                deviceSessionService.getAllSessionsForAdmin(authentication.getName());
        return ResponseEntity.ok(ApiResponse.success("All student & user device login sessions retrieved", sessions));
    }

    @PostMapping("/sessions/{id}/revoke")
    public ResponseEntity<ApiResponse<String>> revokeSession(
            Authentication authentication,
            @PathVariable UUID id
    ) {
        if (authentication == null || authentication.getName() == null) {
            return ResponseEntity.status(401).body(ApiResponse.error("Unauthorized"));
        }
        deviceSessionService.revokeSession(authentication.getName(), id);
        return ResponseEntity.ok(ApiResponse.success("Session revoked successfully", "SUCCESS"));
    }

    @DeleteMapping("/sessions/{id}")
    public ResponseEntity<ApiResponse<String>> deleteSession(
            Authentication authentication,
            @PathVariable UUID id
    ) {
        if (authentication == null || authentication.getName() == null) {
            return ResponseEntity.status(401).body(ApiResponse.error("Unauthorized"));
        }
        deviceSessionService.revokeSession(authentication.getName(), id);
        return ResponseEntity.ok(ApiResponse.success("Session logged out successfully", "SUCCESS"));
    }

    @PutMapping("/profile")
    public ResponseEntity<ApiResponse<User>> updateProfile(
            Authentication authentication,
            @RequestBody UpdateProfileRequest request
    ) {
        if (authentication == null || authentication.getName() == null) {
            return ResponseEntity.status(401).body(ApiResponse.error("Unauthorized"));
        }
        User updatedUser = userService.updateProfile(authentication.getName(), request);
        return ResponseEntity.ok(ApiResponse.success("Profile updated successfully", updatedUser));
    }

    @PutMapping("/{id}/role")
    public ResponseEntity<ApiResponse<User>> updateUserRole(
            Authentication authentication,
            @PathVariable UUID id,
            @RequestBody java.util.Map<String, String> body
    ) {
        if (authentication == null || authentication.getName() == null) {
            return ResponseEntity.status(401).body(ApiResponse.error("Unauthorized: Please log in as Master Admin."));
        }
        String role = body.get("role");
        User updated = userService.updateUserRole(authentication.getName(), id, role);
        return ResponseEntity.ok(ApiResponse.success("User role updated successfully", updated));
    }
}
