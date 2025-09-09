package com.friendavailability.domain.exception;

import org.springframework.http.HttpStatus;

public class GoogleAuthenticationException extends BusinessException {

    public GoogleAuthenticationException(String message) {
        super(message, "GOOGLE_AUTH_ERROR", HttpStatus.UNAUTHORIZED);
    }

    public GoogleAuthenticationException(String message, Throwable cause) {
        super(message, "GOOGLE_AUTH_ERROR", HttpStatus.UNAUTHORIZED, cause);
    }

    public static GoogleAuthenticationException invalidToken() {
        return new GoogleAuthenticationException("Invalid or expired Google token");
    }

    public static GoogleAuthenticationException missingCredential() {
        return new GoogleAuthenticationException("Google credential is required");
    }

    public static GoogleAuthenticationException tokenVerificationFailed(String reason) {
        return (GoogleAuthenticationException) new GoogleAuthenticationException("Google token verification failed")
                .withDetail("reason", reason);
    }

    public static GoogleAuthenticationException invalidAudience(String expectedAudience, String actualAudience) {
        return (GoogleAuthenticationException) new GoogleAuthenticationException("Token audience mismatch")
                .withDetail("expected", expectedAudience)
                .withDetail("actual", actualAudience);
    }
}