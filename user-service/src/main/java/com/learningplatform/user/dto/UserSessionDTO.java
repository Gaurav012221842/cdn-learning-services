package com.learningplatform.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.ZonedDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserSessionDTO {
    private UUID id;
    private UUID userId;
    private String userEmail;
    private String fullName;
    private String role;
    private String deviceType;
    private String os;
    private String browser;
    private String ipAddress;
    private String location;
    private Boolean isActive;
    private Boolean isCurrentDevice;
    private ZonedDateTime loginAt;
    private ZonedDateTime lastActiveAt;

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

    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }

    public Boolean getIsCurrentDevice() { return isCurrentDevice; }
    public void setIsCurrentDevice(Boolean isCurrentDevice) { this.isCurrentDevice = isCurrentDevice; }

    public ZonedDateTime getLoginAt() { return loginAt; }
    public void setLoginAt(ZonedDateTime loginAt) { this.loginAt = loginAt; }

    public ZonedDateTime getLastActiveAt() { return lastActiveAt; }
    public void setLastActiveAt(ZonedDateTime lastActiveAt) { this.lastActiveAt = lastActiveAt; }
}
