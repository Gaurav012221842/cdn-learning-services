package com.learningplatform.common.constants;

/**
 * Standard HTTP headers used across API Gateway and downstream microservices.
 */
public final class SecurityHeaders {

    private SecurityHeaders() {}

    public static final String USER_ID_HEADER = "X-User-Id";
    public static final String USER_EMAIL_HEADER = "X-User-Email";
    public static final String USER_ROLE_HEADER = "X-User-Role";
    public static final String CORRELATION_ID_HEADER = "X-Correlation-Id";
}
