package com.learningplatform.user.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.ZonedDateTime;
import java.util.UUID;

@Entity
@Table(name = "user_device_sessions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDeviceSession {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "user_email", nullable = false)
    private String userEmail;

    @Column(name = "full_name")
    private String fullName;

    @Column(nullable = false)
    private String role;

    @Column(name = "device_type")
    private String deviceType; // DESKTOP, MOBILE, TABLET

    @Column(name = "os")
    private String os; // macOS, Windows, Linux, iOS, Android

    @Column(name = "browser")
    private String browser; // Chrome, Safari, Firefox, Edge, etc.

    @Column(name = "user_agent", columnDefinition = "TEXT")
    private String userAgent;

    @Column(name = "ip_address")
    private String ipAddress;

    @Column(name = "location")
    private String location;

    @Column(name = "token", columnDefinition = "TEXT")
    private String token;

    @Column(name = "token_snippet")
    private String tokenSnippet;

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "login_at")
    private ZonedDateTime loginAt;

    @Column(name = "last_active_at")
    private ZonedDateTime lastActiveAt;

    @PrePersist
    public void prePersist() {
        if (id == null) id = UUID.randomUUID();
        if (loginAt == null) loginAt = ZonedDateTime.now();
        if (lastActiveAt == null) lastActiveAt = ZonedDateTime.now();
        if (isActive == null) isActive = true;
        if (deviceType == null) deviceType = "DESKTOP";
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }

    public String getUserEmail() { return userEmail; }
    public void setUserEmail(String userEmail) { this.userEmail = userEmail; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getDeviceType() { return deviceType; }
    public void setDeviceType(String deviceType) { this.deviceType = deviceType; }

    public String getOs() { return os; }
    public void setOs(String os) { this.os = os; }

    public String getBrowser() { return browser; }
    public void setBrowser(String browser) { this.browser = browser; }

    public String getUserAgent() { return userAgent; }
    public void setUserAgent(String userAgent) { this.userAgent = userAgent; }

    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public String getTokenSnippet() { return tokenSnippet; }
    public void setTokenSnippet(String tokenSnippet) { this.tokenSnippet = tokenSnippet; }

    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }

    public ZonedDateTime getLoginAt() { return loginAt; }
    public void setLoginAt(ZonedDateTime loginAt) { this.loginAt = loginAt; }

    public ZonedDateTime getLastActiveAt() { return lastActiveAt; }
    public void setLastActiveAt(ZonedDateTime lastActiveAt) { this.lastActiveAt = lastActiveAt; }
}
