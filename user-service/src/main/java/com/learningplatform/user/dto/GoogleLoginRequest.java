package com.learningplatform.user.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GoogleLoginRequest {
    private String credential;
    private String email;
    private String fullName;
    private String avatarUrl;
    private String googleId;
    private String role;
    private Boolean isRegister;
}
