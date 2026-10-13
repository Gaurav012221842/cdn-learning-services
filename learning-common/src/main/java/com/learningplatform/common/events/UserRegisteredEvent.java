package com.learningplatform.common.events;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

/**
 * Emitted when a new user registers on the platform.
 * Topic: user-registered-events
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserRegisteredEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    private UUID userId;
    private String email;
    private String fullName;
    private String role;
    private Instant registeredAt;
}
