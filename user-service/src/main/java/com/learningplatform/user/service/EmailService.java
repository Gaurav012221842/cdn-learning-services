package com.learningplatform.user.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${site.title:Gaurav's CDN Learning Platform}")
    private String siteTitle;

    @Value("${app.frontend-url:http://localhost:3000}")
    private String frontendUrl;

    @Value("${brevo.api-key:${BREVO_API_KEY:}}")
    private String brevoApiKey;

    @Value("${brevo.sender-email:${BREVO_SENDER_EMAIL:${spring.mail.username:${MAIL_USERNAME:serversidegaurav@gmail.com}}}}")
    private String brevoSenderEmail;

    @Value("${brevo.sender-name:${BREVO_SENDER_NAME:${site.title:Gaurav's CDN Learning Platform}}}")
    private String brevoSenderName;

    @Async
    public void sendPasswordResetEmail(String toEmail, String otpCode) {
        String cleanFrontendUrl = frontendUrl.endsWith("/") ? frontendUrl.substring(0, frontendUrl.length() - 1) : frontendUrl;
        String resetUrl = cleanFrontendUrl + "/reset-password?email=" + URLEncoder.encode(toEmail, StandardCharsets.UTF_8) + "&token=" + otpCode;
        String subject = "🔐 Password Reset Verification Code - " + siteTitle;

        String htmlContent = "<div style=\"font-family: 'Segoe UI', Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 24px; background: #f8fafc; border-radius: 16px; border: 1px solid #e2e8f0;\">"
                + "<div style=\"text-align: center; margin-bottom: 24px;\">"
                + "<h2 style=\"color: #4f46e5; margin: 0; font-size: 24px;\">" + siteTitle + "</h2>"
                + "<p style=\"color: #64748b; font-size: 14px; margin-top: 4px;\">Secure Account Recovery</p>"
                + "</div>"
                + "<div style=\"background: #ffffff; padding: 24px; border-radius: 12px; box-shadow: 0 4px 12px rgba(0,0,0,0.05); text-align: center;\">"
                + "<p style=\"font-size: 15px; color: #334155; line-height: 1.6;\">You requested a password reset for your account. Click the button below to reset your password directly, or copy the 6-digit code:</p>"
                + "<div style=\"margin: 20px 0;\">"
                + "<a href=\"" + resetUrl + "\" style=\"background: #4f46e5; color: #ffffff; padding: 12px 24px; border-radius: 10px; text-decoration: none; font-weight: 800; font-size: 14px; display: inline-block;\">🔗 Reset Password Now</a>"
                + "</div>"
                + "<p style=\"font-size: 13px; color: #64748b; margin-top: 16px;\">Or enter this code manually on the reset page:</p>"
                + "<div style=\"margin: 16px 0; padding: 14px; background: #f1f5f9; border-radius: 12px; font-size: 28px; font-weight: 800; letter-spacing: 6px; color: #4f46e5; font-family: monospace;\">"
                + otpCode
                + "</div>"
                + "<p style=\"font-size: 13px; color: #94a3b8;\">This link and code are valid for <strong>15 minutes</strong>. If you did not request a password reset, please ignore this email.</p>"
                + "</div>"
                + "<div style=\"text-align: center; margin-top: 24px; font-size: 12px; color: #94a3b8;\">"
                + "&copy; 2026 " + siteTitle + ". All rights reserved."
                + "</div>"
                + "</div>";

        // 1. Send via Brevo HTTPS REST API (Port 443 - Bypasses Render SMTP port blocking)
        if (sendViaBrevoHttp(toEmail, subject, htmlContent)) {
            return;
        }

        // 2. Fallback to JavaMailSender (SMTP)
        sendViaSmtp(toEmail, subject, htmlContent);
    }

    @Async
    public void sendRegistrationOtpEmail(String toEmail, String otpCode) {
        String subject = "📧 Registration Verification Code - " + siteTitle;

        String htmlContent = "<div style=\"font-family: 'Segoe UI', Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 24px; background: #f8fafc; border-radius: 16px; border: 1px solid #e2e8f0;\">"
                + "<div style=\"text-align: center; margin-bottom: 24px;\">"
                + "<h2 style=\"color: #4f46e5; margin: 0; font-size: 24px;\">" + siteTitle + "</h2>"
                + "<p style=\"color: #64748b; font-size: 14px; margin-top: 4px;\">Account Registration Verification</p>"
                + "</div>"
                + "<div style=\"background: #ffffff; padding: 24px; border-radius: 12px; box-shadow: 0 4px 12px rgba(0,0,0,0.05); text-align: center;\">"
                + "<p style=\"font-size: 15px; color: #334155; line-height: 1.6;\">Thank you for creating an account! Please enter the 6-digit verification code below to verify your email address and complete registration:</p>"
                + "<div style=\"margin: 20px 0; padding: 14px; background: #f1f5f9; border-radius: 12px; font-size: 32px; font-weight: 800; letter-spacing: 8px; color: #4f46e5; font-family: monospace;\">"
                + otpCode
                + "</div>"
                + "<p style=\"font-size: 13px; color: #94a3b8;\">This verification code is valid for <strong>15 minutes</strong>. If you did not initiate this request, please ignore this email.</p>"
                + "</div>"
                + "<div style=\"text-align: center; margin-top: 24px; font-size: 12px; color: #94a3b8;\">"
                + "&copy; 2026 " + siteTitle + ". All rights reserved."
                + "</div>"
                + "</div>";

        // 1. Send via Brevo HTTPS REST API (Port 443 - Bypasses Render SMTP port blocking)
        if (sendViaBrevoHttp(toEmail, subject, htmlContent)) {
            return;
        }

        // 2. Fallback to JavaMailSender (SMTP)
        sendViaSmtp(toEmail, subject, htmlContent);
    }

    private boolean sendViaBrevoHttp(String toEmail, String subject, String htmlContent) {
        if (brevoApiKey == null || brevoApiKey.isBlank()) {
            return false;
        }
        try {
            Map<String, Object> payloadMap = Map.of(
                    "sender", Map.of("name", brevoSenderName, "email", brevoSenderEmail),
                    "to", List.of(Map.of("email", toEmail)),
                    "subject", subject,
                    "htmlContent", htmlContent
            );

            String jsonPayload = objectMapper.writeValueAsString(payloadMap);

            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.brevo.com/v3/smtp/email"))
                    .header("api-key", brevoApiKey.trim())
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                log.info("Email [{}] sent successfully via Brevo HTTPS REST API (Port 443) to {}", subject, toEmail);
                return true;
            } else {
                log.warn("Brevo API returned status {}: {}", response.statusCode(), response.body());
                return false;
            }
        } catch (Exception e) {
            log.warn("Failed to send via Brevo HTTPS API: {}", e.getMessage());
            return false;
        }
    }

    private void sendViaSmtp(String toEmail, String subject, String htmlContent) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(brevoSenderEmail, siteTitle);
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("Email [{}] sent successfully via JavaMailSender (SMTP) to {}", subject, toEmail);
        } catch (Exception e) {
            log.error("Failed to send email via JavaMailSender (SMTP) to {}: {}", toEmail, e.getMessage(), e);
        }
    }
}
