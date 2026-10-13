package com.learningplatform.user.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SendRegistrationOtpRequest {
    private String email;
    private String password;
    private String fullName;
    private String role;
}
