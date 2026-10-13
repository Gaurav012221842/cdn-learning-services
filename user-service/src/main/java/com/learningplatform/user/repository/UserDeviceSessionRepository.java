package com.learningplatform.user.repository;

import com.learningplatform.user.entity.UserDeviceSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserDeviceSessionRepository extends JpaRepository<UserDeviceSession, UUID> {

    List<UserDeviceSession> findByUserIdOrderByLoginAtDesc(UUID userId);

    List<UserDeviceSession> findByUserEmailOrderByLoginAtDesc(String userEmail);

    List<UserDeviceSession> findAllByOrderByLoginAtDesc();

    List<UserDeviceSession> findByRoleOrderByLoginAtDesc(String role);

    List<UserDeviceSession> findByTokenSnippet(String tokenSnippet);

    List<UserDeviceSession> findByToken(String token);
}
