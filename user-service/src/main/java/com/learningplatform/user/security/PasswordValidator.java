package com.learningplatform.user.security;

import java.util.regex.Pattern;

public class PasswordValidator {

    private static final Pattern UPPERCASE_PATTERN = Pattern.compile("[A-Z]");
    private static final Pattern LOWERCASE_PATTERN = Pattern.compile("[a-z]");
    private static final Pattern DIGIT_PATTERN = Pattern.compile("[0-9]");
    private static final Pattern SPECIAL_CHAR_PATTERN = Pattern.compile("[!@#$%^&*(),.?\":{}|<>]");

    public static void validate(String password) {
        if (password == null || password.length() < 8) {
            throw new IllegalArgumentException("Password must be at least 8 characters long");
        }
        if (!UPPERCASE_PATTERN.matcher(password).find()) {
            throw new IllegalArgumentException("Password must contain at least one uppercase letter (A-Z)");
        }
        if (!LOWERCASE_PATTERN.matcher(password).find()) {
            throw new IllegalArgumentException("Password must contain at least one lowercase letter (a-z)");
        }
        if (!DIGIT_PATTERN.matcher(password).find()) {
            throw new IllegalArgumentException("Password must contain at least one digit (0-9)");
        }
        if (!SPECIAL_CHAR_PATTERN.matcher(password).find()) {
            throw new IllegalArgumentException("Password must contain at least one special character (!@#$%^&*...)");
        }
    }
}
