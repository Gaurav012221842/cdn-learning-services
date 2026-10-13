package com.learningplatform.user.service;

import com.learningplatform.common.exception.BusinessException;
import com.learningplatform.user.entity.User;
import com.learningplatform.user.entity.UserDeviceSession;
import com.learningplatform.user.repository.UserDeviceSessionRepository;
import com.learningplatform.user.repository.UserRepository;
import com.learningplatform.user.security.TokenBlacklistService;
import com.learningplatform.user.dto.UserSessionDTO;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DeviceSessionService {

    private final UserDeviceSessionRepository sessionRepository;
    private final UserRepository userRepository;
    private final TokenBlacklistService tokenBlacklistService;

    public void recordLoginSession(User user, String token, HttpServletRequest request) {
        try {
            String userAgent = request != null ? request.getHeader("User-Agent") : "Unknown";
            String ipAddress = extractClientIp(request);
            String os = parseOperatingSystem(userAgent);
            String browser = parseBrowser(userAgent);
            String deviceType = parseDeviceType(userAgent);
            String location = resolveLocation(ipAddress);
            String tokenSnippet = extractTokenSnippet(token);

            UserDeviceSession session = UserDeviceSession.builder()
                    .id(UUID.randomUUID())
                    .userId(user.getId())
                    .userEmail(user.getEmail())
                    .fullName(user.getFullName())
                    .role(user.getRole())
                    .deviceType(deviceType)
                    .os(os)
                    .browser(browser)
                    .userAgent(userAgent)
                    .ipAddress(ipAddress)
                    .location(location)
                    .token(token)
                    .tokenSnippet(tokenSnippet)
                    .isActive(true)
                    .loginAt(ZonedDateTime.now())
                    .lastActiveAt(ZonedDateTime.now())
                    .build();

            sessionRepository.save(session);
            log.info("Recorded login device session for user: {} on {} ({}) from IP: {}",
                    user.getEmail(), os, browser, ipAddress);
        } catch (Exception e) {
            log.error("Failed to record user device session: {}", e.getMessage(), e);
        }
    }

    public List<UserSessionDTO> getUserSessions(String userEmail, String authHeader) {
        String currentTokenSnippet = null;
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            currentTokenSnippet = extractTokenSnippet(authHeader.substring(7));
        }

        List<UserDeviceSession> sessions = sessionRepository.findByUserEmailOrderByLoginAtDesc(userEmail);
        final String finalSnippet = currentTokenSnippet;

        boolean currentMatched = false;
        List<UserSessionDTO> dtos = new java.util.ArrayList<>();

        for (int i = 0; i < sessions.size(); i++) {
            UserDeviceSession s = sessions.get(i);
            boolean isCurrent = false;
            if (finalSnippet != null && finalSnippet.equals(s.getTokenSnippet())) {
                isCurrent = true;
                currentMatched = true;
            }

            dtos.add(toDTO(s, isCurrent));
        }

        // If no token snippet match found and list has sessions, mark the most recent active session as current device
        if (!currentMatched && !dtos.isEmpty()) {
            for (UserSessionDTO dto : dtos) {
                if (Boolean.TRUE.equals(dto.getIsActive())) {
                    dto.setIsCurrentDevice(true);
                    break;
                }
            }
        }

        return dtos;
    }

    public List<UserSessionDTO> getAllSessionsForAdmin(String callerEmail) {
        User caller = userRepository.findByEmail(callerEmail)
                .orElseThrow(() -> new BusinessException("Admin user not found"));

        if (!"ADMIN".equalsIgnoreCase(caller.getRole())) {
            throw new BusinessException("Access denied: Admin role required to view all session history.");
        }

        return sessionRepository.findAllByOrderByLoginAtDesc().stream()
                .map(s -> toDTO(s, false))
                .collect(Collectors.toList());
    }

    @Transactional
    public void revokeSession(String userEmail, UUID sessionId) {
        UserDeviceSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new BusinessException("Session not found: " + sessionId));

        User caller = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new BusinessException("User not found: " + userEmail));

        boolean isAdmin = "ADMIN".equalsIgnoreCase(caller.getRole());
        if (!isAdmin && !session.getUserEmail().equalsIgnoreCase(userEmail)) {
            throw new BusinessException("You are not authorized to revoke this session.");
        }

        session.setIsActive(false);
        session.setLastActiveAt(ZonedDateTime.now());
        sessionRepository.save(session);

        if (session.getToken() != null && !session.getToken().isBlank()) {
            tokenBlacklistService.blacklist(session.getToken());
        }
        if (session.getTokenSnippet() != null && !session.getTokenSnippet().isBlank()) {
            tokenBlacklistService.blacklist(session.getTokenSnippet());
        }

        log.info("Session {} for user {} was marked inactive & blacklisted by {}", sessionId, session.getUserEmail(), userEmail);
    }

    public boolean isTokenRevoked(String token) {
        if (token == null || token.isBlank()) return true;
        if (tokenBlacklistService.isBlacklisted(token)) return true;

        String snippet = extractTokenSnippet(token);
        if (snippet != null && tokenBlacklistService.isBlacklisted(snippet)) {
            return true;
        }

        if (snippet != null) {
            try {
                List<UserDeviceSession> sessions = sessionRepository.findByTokenSnippet(snippet);
                if (sessions != null && !sessions.isEmpty()) {
                    for (UserDeviceSession s : sessions) {
                        if (Boolean.FALSE.equals(s.getIsActive())) {
                            tokenBlacklistService.blacklist(token);
                            tokenBlacklistService.blacklist(snippet);
                            return true;
                        }
                    }
                }
            } catch (Exception e) {
                log.warn("Error checking token session status: {}", e.getMessage());
            }
        }
        return false;
    }

    private UserSessionDTO toDTO(UserDeviceSession s, boolean isCurrent) {
        return UserSessionDTO.builder()
                .id(s.getId())
                .userId(s.getUserId())
                .userEmail(s.getUserEmail())
                .fullName(s.getFullName())
                .role(s.getRole())
                .deviceType(s.getDeviceType())
                .os(s.getOs())
                .browser(s.getBrowser())
                .ipAddress(s.getIpAddress())
                .location(s.getLocation())
                .isActive(s.getIsActive())
                .isCurrentDevice(isCurrent)
                .loginAt(s.getLoginAt())
                .lastActiveAt(s.getLastActiveAt())
                .build();
    }

    private String extractTokenSnippet(String token) {
        if (token == null || token.isBlank()) return null;
        String clean = token.trim();
        int lastDot = clean.lastIndexOf('.');
        if (lastDot > 0 && lastDot < clean.length() - 1) {
            String sig = clean.substring(lastDot + 1);
            return sig.length() > 24 ? sig.substring(0, 24) : sig;
        }
        return clean.length() > 24 ? clean.substring(clean.length() - 24) : clean;
    }

    public String extractClientIp(HttpServletRequest request) {
        if (request == null) return "127.0.0.1";

        String[] headers = {
                "X-Forwarded-For",
                "CF-Connecting-IP",
                "X-Real-IP",
                "Proxy-Client-IP",
                "WL-Proxy-Client-IP",
                "HTTP_CLIENT_IP",
                "HTTP_X_FORWARDED_FOR"
        };

        for (String header : headers) {
            String ip = request.getHeader(header);
            if (ip != null && !ip.isBlank() && !"unknown".equalsIgnoreCase(ip.trim())) {
                if (ip.contains(",")) {
                    ip = ip.split(",")[0].trim();
                }
                return ip;
            }
        }

        String remote = request.getRemoteAddr();
        if ("0:0:0:0:0:0:0:1".equals(remote) || "::1".equals(remote)) {
            return "127.0.0.1";
        }
        return remote != null ? remote : "127.0.0.1";
    }

    public String parseOperatingSystem(String userAgent) {
        if (userAgent == null || userAgent.isBlank()) return "Unknown OS";
        String ua = userAgent.toLowerCase();

        if (ua.contains("mac os") || ua.contains("macintosh") || ua.contains("darwin")) {
            return "macOS";
        } else if (ua.contains("windows nt 10.0")) {
            return "Windows 10/11";
        } else if (ua.contains("windows nt 6.3")) {
            return "Windows 8.1";
        } else if (ua.contains("windows nt 6.1")) {
            return "Windows 7";
        } else if (ua.contains("windows")) {
            return "Windows";
        } else if (ua.contains("iphone")) {
            return "iOS (iPhone)";
        } else if (ua.contains("ipad")) {
            return "iPadOS (iPad)";
        } else if (ua.contains("android")) {
            return "Android";
        } else if (ua.contains("linux")) {
            return "Linux";
        } else if (ua.contains("cros")) {
            return "ChromeOS";
        }
        return "Unknown OS";
    }

    public String parseBrowser(String userAgent) {
        if (userAgent == null || userAgent.isBlank()) return "Unknown Browser";
        String ua = userAgent;

        if (ua.contains("Edg/")) {
            return "Microsoft Edge";
        } else if (ua.contains("Chrome/") && !ua.contains("Edg/") && !ua.contains("OPR/")) {
            return "Google Chrome";
        } else if (ua.contains("Safari/") && !ua.contains("Chrome/")) {
            return "Apple Safari";
        } else if (ua.contains("Firefox/")) {
            return "Mozilla Firefox";
        } else if (ua.contains("OPR/") || ua.contains("Opera/")) {
            return "Opera";
        } else if (ua.contains("PostmanRuntime")) {
            return "Postman API Client";
        }
        return "Web Browser";
    }

    public String parseDeviceType(String userAgent) {
        if (userAgent == null || userAgent.isBlank()) return "DESKTOP";
        String ua = userAgent.toLowerCase();

        if (ua.contains("ipad") || ua.contains("tablet")) {
            return "TABLET";
        } else if (ua.contains("mobile") || ua.contains("iphone") || (ua.contains("android") && !ua.contains("tablet"))) {
            return "MOBILE";
        }
        return "DESKTOP";
    }

    private String resolveLocation(String ip) {
        if (ip == null || ip.equals("127.0.0.1") || ip.startsWith("192.168.") || ip.startsWith("10.")) {
            return "Local Network (LAN)";
        }
        return "Online / Cloud";
    }
}
