package com.learningplatform.user.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VerifyRegistrationOtpRequest {
    private String email;
    private String otpCode;
    private String password;
    private String fullName;
    private String role;
}
