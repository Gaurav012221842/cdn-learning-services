package com.learningplatform.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {
    private UUID id;
    private String token;
    private String email;
    private String fullName;
    private String role;
    private String avatarUrl;
}
