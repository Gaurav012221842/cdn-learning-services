package com.learningplatform.user.service;

import com.learningplatform.common.exception.BusinessException;
import com.learningplatform.user.dto.UpdateProfileRequest;
import com.learningplatform.user.entity.User;
import com.learningplatform.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    @Value("${app.security.master-admin-email:serversidegaurav@gmail.com}")
    private String masterAdminEmail;

    public boolean isMasterAdmin(String email) {
        if (email == null || masterAdminEmail == null) return false;
        return email.trim().equalsIgnoreCase(masterAdminEmail.trim());
    }

    public List<User> getAllUsers() {
        List<User> users = userRepository.findAll();
        for (User u : users) {
            if (isMasterAdmin(u.getEmail()) && !"ADMIN".equalsIgnoreCase(u.getRole())) {
                u.setRole("ADMIN");
                userRepository.save(u);
            }
        }
        return users;
    }

    public User getUserById(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found: " + id));
    }

    public User getUserByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found with email: " + email));
        if (isMasterAdmin(user.getEmail()) && !"ADMIN".equalsIgnoreCase(user.getRole())) {
            user.setRole("ADMIN");
            user = userRepository.save(user);
        }
        return user;
    }

    public User updateProfile(String userEmail, UpdateProfileRequest request) {
        User user = getUserByEmail(userEmail);
        if (request.getFullName() != null && !request.getFullName().isBlank()) {
            user.setFullName(request.getFullName());
        }
        if (request.getAvatarUrl() != null && !request.getAvatarUrl().isBlank()) {
            String avatarUrl = request.getAvatarUrl().trim();
            try {
                URI uri = URI.create(avatarUrl);
                String scheme = uri.getScheme();
                if (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme)) {
                    throw new BusinessException("Avatar URL must be a valid http(s) URL");
                }
                user.setAvatarUrl(avatarUrl);
            } catch (IllegalArgumentException ex) {
                throw new BusinessException("Avatar URL must be a valid http(s) URL");
            }
        }
        user.setUpdatedAt(ZonedDateTime.now());
        return userRepository.save(user);
    }

    public User updateUserRole(String callerEmail, UUID targetUserId, String newRole) {
        if (!isMasterAdmin(callerEmail)) {
            throw new BusinessException("Forbidden: Only the Master Admin (" + masterAdminEmail + ") can change user roles.");
        }

        if (newRole == null || (!"ADMIN".equalsIgnoreCase(newRole) && !"STUDENT".equalsIgnoreCase(newRole) && !"INSTRUCTOR".equalsIgnoreCase(newRole))) {
            throw new BusinessException("Invalid role specified. Supported roles: STUDENT, ADMIN, INSTRUCTOR");
        }

        User targetUser = getUserById(targetUserId);
        if (isMasterAdmin(targetUser.getEmail()) && !"ADMIN".equalsIgnoreCase(newRole)) {
            throw new BusinessException("Forbidden: Cannot demote the Master Admin account.");
        }

        targetUser.setRole(newRole.trim().toUpperCase());
        targetUser.setUpdatedAt(ZonedDateTime.now());
        return userRepository.save(targetUser);
    }
}
