package com.learningplatform.user.controller;

import com.learningplatform.common.dto.ApiResponse;
import com.learningplatform.user.dto.*;
import com.learningplatform.user.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    @Value("${app.security.cookie-secure:false}")
    private boolean cookieSecure;

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(
            @RequestBody RegisterRequest request,
            jakarta.servlet.http.HttpServletRequest httpRequest
    ) {
        AuthResponse response = authService.register(request, httpRequest);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, buildAuthCookie(response.getToken(), httpRequest).toString())
                .body(ApiResponse.success("User registered successfully", response));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @RequestBody LoginRequest request,
            jakarta.servlet.http.HttpServletRequest httpRequest
    ) {
        AuthResponse response = authService.login(request, httpRequest);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, buildAuthCookie(response.getToken(), httpRequest).toString())
                .body(ApiResponse.success("Login successful", response));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<String>> logout(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            jakarta.servlet.http.HttpServletResponse httpResponse
    ) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Missing or invalid Authorization header"));
        }
        String token = authHeader.substring(7);
        authService.logout(token);
        httpResponse.addHeader(HttpHeaders.SET_COOKIE, clearAuthCookie().toString());
        return ResponseEntity.ok(ApiResponse.success("Logout successful", "SUCCESS"));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<String>> forgotPassword(@RequestBody ForgotPasswordRequest request) {
        String token = authService.forgotPassword(request);
        return ResponseEntity.ok(ApiResponse.success("Password reset instructions sent", token));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<String>> resetPassword(@RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ResponseEntity.ok(ApiResponse.success("Password reset successfully. You can now login with your new password.", "SUCCESS"));
    }

    @PostMapping("/change-password")
    public ResponseEntity<ApiResponse<String>> changePassword(@RequestBody ChangePasswordRequest request) {
        authService.changePassword(request);
        return ResponseEntity.ok(ApiResponse.success("Password changed successfully", "SUCCESS"));
    }

    @PostMapping("/send-registration-otp")
    public ResponseEntity<ApiResponse<String>> sendRegistrationOtp(@RequestBody SendRegistrationOtpRequest request) {
        authService.sendRegistrationOtp(request);
        return ResponseEntity.ok(ApiResponse.success("Verification code sent to your email.", "SUCCESS"));
    }

    @PostMapping("/verify-registration-otp")
    public ResponseEntity<ApiResponse<AuthResponse>> verifyRegistrationOtpAndRegister(
            @RequestBody VerifyRegistrationOtpRequest request,
            jakarta.servlet.http.HttpServletRequest httpRequest
    ) {
        AuthResponse response = authService.verifyRegistrationOtpAndRegister(request, httpRequest);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, buildAuthCookie(response.getToken(), httpRequest).toString())
                .body(ApiResponse.success("Account registered and verified successfully!", response));
    }

    @PostMapping("/google")
    public ResponseEntity<ApiResponse<AuthResponse>> googleLogin(
            @RequestBody GoogleLoginRequest request,
            jakarta.servlet.http.HttpServletRequest httpRequest
    ) {
        AuthResponse response = authService.googleLogin(request, httpRequest);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, buildAuthCookie(response.getToken(), httpRequest).toString())
                .body(ApiResponse.success("Google authentication successful!", response));
    }

    @GetMapping("/validate")
    public ResponseEntity<ApiResponse<AuthResponse>> validateToken(@RequestHeader("Authorization") String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Missing or invalid Authorization header"));
        }
        String token = authHeader.substring(7);
        AuthResponse response = authService.validateToken(token);
        return ResponseEntity.ok(ApiResponse.success("Token is valid", response));
    }

    private ResponseCookie buildAuthCookie(String token, jakarta.servlet.http.HttpServletRequest request) {
        boolean secure = cookieSecure || (request != null && request.isSecure());
        return ResponseCookie.from("token", token)
                .httpOnly(true)
                .secure(secure)
                .sameSite("Lax")
                .path("/")
                .maxAge(Duration.ofDays(1))
                .build();
    }

    private ResponseCookie clearAuthCookie() {
        return ResponseCookie.from("token", "")
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite("Lax")
                .path("/")
                .maxAge(0)
                .build();
    }
}
