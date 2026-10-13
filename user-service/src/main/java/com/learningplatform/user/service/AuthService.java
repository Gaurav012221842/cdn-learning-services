package com.learningplatform.user.service;

import com.learningplatform.user.security.JwtService;
import com.learningplatform.user.security.TokenBlacklistService;
import com.learningplatform.user.entity.User;
import com.learningplatform.user.repository.UserRepository;
import com.learningplatform.user.dto.*;
import com.learningplatform.user.producer.UserKafkaProducer;
import com.learningplatform.user.security.PasswordValidator;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.ZonedDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final TokenBlacklistService tokenBlacklistService;
    private final EmailService emailService;
    private final StringRedisTemplate stringRedisTemplate;
    private final DeviceSessionService deviceSessionService;
    private final UserKafkaProducer userKafkaProducer;

    @org.springframework.beans.factory.annotation.Value("${app.security.master-admin-email:serversidegaurav@gmail.com}")
    private String masterAdminEmail;

    public boolean isMasterAdmin(String email) {
        if (email == null || masterAdminEmail == null) return false;
        return email.trim().equalsIgnoreCase(masterAdminEmail.trim());
    }

    // In-memory OTP code cache for password resets with 15-minute expiration
    private final Map<String, ResetCodeInfo> resetCodeStore = new ConcurrentHashMap<>();

    // In-memory OTP code cache for pending user registrations
    private final Map<String, RegistrationOtpInfo> registrationOtpStore = new ConcurrentHashMap<>();

    @Data
    @AllArgsConstructor
    private static class ResetCodeInfo {
        private String code;
        private ZonedDateTime expiresAt;
    }

    @Data
    @AllArgsConstructor
    private static class RegistrationOtpInfo {
        private String code;
        private String password;
        private String fullName;
        private String role;
        private ZonedDateTime expiresAt;
    }

    public AuthResponse register(RegisterRequest request) {
        return register(request, null);
    }

    public AuthResponse register(RegisterRequest request, jakarta.servlet.http.HttpServletRequest httpRequest) {
        if (request == null || request.getEmail() == null || request.getPassword() == null) {
            throw new IllegalArgumentException("Email and password are required");
        }

        String email = request.getEmail().trim();
        if (userRepository.findByEmail(email).isPresent()) {
            throw new IllegalArgumentException("User already exists with this email");
        }

        // Enforce strong password policy: Uppercase, Lowercase, Number, Symbol, 8+ chars
        PasswordValidator.validate(request.getPassword());

        String assignedRole = resolveRegistrationRole(email);

        User user = User.builder()
                .id(UUID.randomUUID())
                .email(email)
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName())
                .role(assignedRole)
                .build();

        User savedUser = userRepository.save(user);
        org.springframework.security.core.userdetails.User userDetails =
                new org.springframework.security.core.userdetails.User(
                        savedUser.getEmail(),
                        savedUser.getPasswordHash(),
                        java.util.Collections.singletonList(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_" + savedUser.getRole()))
                );
        String jwtToken = jwtService.generateToken(userDetails);

        if (deviceSessionService != null) {
            deviceSessionService.recordLoginSession(savedUser, jwtToken, httpRequest);
        }

        if (userKafkaProducer != null) {
            userKafkaProducer.publishUserRegistered(savedUser.getId(), savedUser.getEmail(), savedUser.getFullName(), savedUser.getRole());
        }

        return AuthResponse.builder()
                .id(savedUser.getId())
                .token(jwtToken)
                .email(savedUser.getEmail())
                .fullName(savedUser.getFullName())
                .role(savedUser.getRole())
                .avatarUrl(savedUser.getAvatarUrl())
                .build();
    }

    public AuthResponse login(LoginRequest request) {
        return login(request, null);
    }

    public AuthResponse login(LoginRequest request, jakarta.servlet.http.HttpServletRequest httpRequest) {
        if (request == null || request.getEmail() == null || request.getPassword() == null) {
            throw new IllegalArgumentException("Email and password are required");
        }

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail().trim(), request.getPassword())
        );
        User user = userRepository.findByEmail(request.getEmail().trim())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (isMasterAdmin(user.getEmail()) && !"ADMIN".equalsIgnoreCase(user.getRole())) {
            user.setRole("ADMIN");
            user = userRepository.save(user);
        }

        org.springframework.security.core.userdetails.User userDetails =
                new org.springframework.security.core.userdetails.User(
                        user.getEmail(),
                        user.getPasswordHash(),
                        java.util.Collections.singletonList(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_" + user.getRole()))
                );
        String jwtToken = jwtService.generateToken(userDetails);

        if (deviceSessionService != null) {
            deviceSessionService.recordLoginSession(user, jwtToken, httpRequest);
        }

        return AuthResponse.builder()
                .id(user.getId())
                .token(jwtToken)
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole())
                .avatarUrl(user.getAvatarUrl())
                .build();
    }

    public void logout(String token) {
        tokenBlacklistService.blacklist(token);
    }

    public String forgotPassword(ForgotPasswordRequest request) {
        if (request == null || request.getEmail() == null || request.getEmail().isBlank()) {
            throw new IllegalArgumentException("Email is required");
        }

        String email = request.getEmail().trim().toLowerCase();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("No user found with email: " + email));

        // Generate a 6-digit numeric OTP code
        String otpCode = String.format("%06d", new Random().nextInt(999999));
        resetCodeStore.put(email, new ResetCodeInfo(otpCode, ZonedDateTime.now().plusMinutes(15)));

        // Send email via JavaMailSender
        emailService.sendPasswordResetEmail(user.getEmail(), otpCode);
        log.info("Generated reset OTP {} for user {}", otpCode, email);

        return otpCode;
    }

    public void resetPassword(ResetPasswordRequest request) {
        if (request == null || request.getEmail() == null || request.getNewPassword() == null) {
            throw new IllegalArgumentException("Email, reset code, and new password are required");
        }

        String email = request.getEmail().trim().toLowerCase();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("No user found with email: " + email));

        // Validate strong password policy
        PasswordValidator.validate(request.getNewPassword());

        // Verify OTP code if present in store
        ResetCodeInfo codeInfo = resetCodeStore.get(email);
        if (codeInfo != null) {
            if (ZonedDateTime.now().isAfter(codeInfo.getExpiresAt())) {
                resetCodeStore.remove(email);
                throw new IllegalArgumentException("Password reset code has expired. Please request a new code.");
            }
            if (request.getToken() != null && !request.getToken().isBlank() && !request.getToken().equalsIgnoreCase(codeInfo.getCode())) {
                throw new IllegalArgumentException("Invalid verification reset code.");
            }
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        resetCodeStore.remove(email);
        log.info("Password successfully reset for user {}", email);
    }

    public AuthResponse validateToken(String token) {
        String userEmail = jwtService.extractUsername(token);
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("Invalid token: User not found"));

        org.springframework.security.core.userdetails.User userDetails =
                new org.springframework.security.core.userdetails.User(
                        user.getEmail(),
                        user.getPasswordHash(),
                        java.util.Collections.singletonList(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_" + user.getRole()))
                );

        if (!jwtService.isTokenValid(token, userDetails)) {
            throw new IllegalArgumentException("Invalid or expired JWT token");
        }

        return AuthResponse.builder()
                .id(user.getId())
                .token(token)
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole())
                .avatarUrl(user.getAvatarUrl())
                .build();
    }

    public void changePassword(ChangePasswordRequest request) {
        if (request == null || request.getEmail() == null || request.getCurrentPassword() == null || request.getNewPassword() == null) {
            throw new IllegalArgumentException("Email, current password, and new password are required");
        }

        String email = request.getEmail().trim().toLowerCase();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("No user found with email: " + email));

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new IllegalArgumentException("Current password is incorrect");
        }

        // Validate strong password policy
        PasswordValidator.validate(request.getNewPassword());

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        log.info("Password changed successfully for user {}", email);
    }

    public void sendRegistrationOtp(SendRegistrationOtpRequest request) {
        if (request == null || request.getEmail() == null || request.getPassword() == null) {
            throw new IllegalArgumentException("Email and password are required");
        }

        String email = request.getEmail().trim().toLowerCase();
        if (userRepository.findByEmail(email).isPresent()) {
            throw new IllegalArgumentException("User already exists with email: " + email + ". Please sign in instead.");
        }

        // Validate password strength policy
        PasswordValidator.validate(request.getPassword());

        String otpCode = String.format("%06d", new Random().nextInt(900000) + 100000);
        ZonedDateTime expiresAt = ZonedDateTime.now().plusMinutes(15);

        // Store OTP in Redis with 15-minute automatic TTL expiration
        try {
            stringRedisTemplate.opsForValue().set("otp:reg:" + email, otpCode, 15, TimeUnit.MINUTES);
        } catch (Exception e) {
            log.warn("Redis OTP save warning: {}", e.getMessage());
        }

        registrationOtpStore.put(email, new RegistrationOtpInfo(
                otpCode,
                request.getPassword(),
                request.getFullName() != null ? request.getFullName() : "Gaurav User",
                request.getRole() != null ? request.getRole() : "STUDENT",
                expiresAt
        ));

        emailService.sendRegistrationOtpEmail(email, otpCode);
        log.info("Registration OTP {} generated, saved to Redis, and email dispatched for {}", otpCode, email);
    }

    public AuthResponse verifyRegistrationOtpAndRegister(
            VerifyRegistrationOtpRequest request,
            jakarta.servlet.http.HttpServletRequest httpRequest
    ) {
        if (request == null || request.getEmail() == null || request.getOtpCode() == null) {
            throw new IllegalArgumentException("Email and verification code are required");
        }

        String email = request.getEmail().trim().toLowerCase();
        RegistrationOtpInfo info = registrationOtpStore.get(email);

        if (info == null) {
            throw new IllegalArgumentException("No pending registration found for " + email + ". Please request a new verification code.");
        }

        if (ZonedDateTime.now().isAfter(info.getExpiresAt())) {
            registrationOtpStore.remove(email);
            throw new IllegalArgumentException("Verification code has expired. Please request a new code.");
        }

        if (!info.getCode().equals(request.getOtpCode().trim())) {
            throw new IllegalArgumentException("Invalid verification code. Please check your email.");
        }

        // Clear verified OTP
        registrationOtpStore.remove(email);

        if (userRepository.findByEmail(email).isPresent()) {
            throw new IllegalArgumentException("User already exists with this email.");
        }

        String rawPassword = request.getPassword() != null ? request.getPassword() : info.getPassword();
        String fullName = request.getFullName() != null && !request.getFullName().isBlank() ? request.getFullName() : info.getFullName();
        String assignedRole = resolveRegistrationRole(email);

        User user = User.builder()
                .id(UUID.randomUUID())
                .email(email)
                .passwordHash(passwordEncoder.encode(rawPassword))
                .fullName(fullName)
                .role(assignedRole)
                .build();

        User savedUser = userRepository.save(user);
        log.info("User {} verified via OTP and created successfully in PostgreSQL as {}", email, assignedRole);

        org.springframework.security.core.userdetails.User userDetails =
                new org.springframework.security.core.userdetails.User(
                        savedUser.getEmail(),
                        savedUser.getPasswordHash(),
                        java.util.Collections.singletonList(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_" + savedUser.getRole()))
                );
        String jwtToken = jwtService.generateToken(userDetails);

        if (deviceSessionService != null) {
            deviceSessionService.recordLoginSession(savedUser, jwtToken, httpRequest);
        }

        return AuthResponse.builder()
                .id(savedUser.getId())
                .token(jwtToken)
                .email(savedUser.getEmail())
                .fullName(savedUser.getFullName())
                .role(savedUser.getRole())
                .avatarUrl(savedUser.getAvatarUrl())
                .build();
    }

    public AuthResponse verifyRegistrationOtpAndRegister(VerifyRegistrationOtpRequest request) {
        return verifyRegistrationOtpAndRegister(request, null);
    }

    public AuthResponse googleLogin(GoogleLoginRequest request) {
        return googleLogin(request, null);
    }

    public AuthResponse googleLogin(GoogleLoginRequest request, jakarta.servlet.http.HttpServletRequest httpRequest) {
        if (request == null || request.getEmail() == null) {
            throw new IllegalArgumentException("Email is required for Google login");
        }

        String email = request.getEmail().trim().toLowerCase();
        boolean isRegisterFlow = Boolean.TRUE.equals(request.getIsRegister());

        Optional<User> existingUserOpt = userRepository.findByEmail(email);

        if (!isRegisterFlow && existingUserOpt.isEmpty()) {
            throw new IllegalArgumentException("No registered account found with email: " + email + ". Please register your account first!");
        }

        User user = existingUserOpt.orElseGet(() -> {
            String assignedRole = resolveRegistrationRole(email);
            log.info("Registering new user via Google OAuth for email {} as {}", email, assignedRole);
            User newUser = User.builder()
                    .id(UUID.randomUUID())
                    .email(email)
                    .passwordHash(passwordEncoder.encode(UUID.randomUUID().toString()))
                    .fullName(request.getFullName() != null ? request.getFullName() : "Google User")
                    .role(assignedRole)
                    .avatarUrl(request.getAvatarUrl())
                    .build();
            return userRepository.save(newUser);
        });

        if (isMasterAdmin(user.getEmail()) && !"ADMIN".equalsIgnoreCase(user.getRole())) {
            user.setRole("ADMIN");
            user = userRepository.save(user);
        }

        // Update avatar if not present
        if ((user.getAvatarUrl() == null || user.getAvatarUrl().isBlank()) && request.getAvatarUrl() != null) {
            user.setAvatarUrl(request.getAvatarUrl());
            userRepository.save(user);
        }

        org.springframework.security.core.userdetails.User userDetails =
                new org.springframework.security.core.userdetails.User(
                        user.getEmail(),
                        user.getPasswordHash(),
                        java.util.Collections.singletonList(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_" + user.getRole()))
                );
        String jwtToken = jwtService.generateToken(userDetails);

        if (deviceSessionService != null) {
            deviceSessionService.recordLoginSession(user, jwtToken, httpRequest);
        }

        return AuthResponse.builder()
                .id(user.getId())
                .token(jwtToken)
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole())
                .avatarUrl(user.getAvatarUrl())
                .build();
    }

    private String resolveRegistrationRole(String email) {
        if (isMasterAdmin(email)) {
            return "ADMIN";
        }
        return "STUDENT";
    }
}
