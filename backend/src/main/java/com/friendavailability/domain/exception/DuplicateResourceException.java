package com.friendavailability.domain.exception;

import org.springframework.http.HttpStatus;

public class DuplicateResourceException extends BusinessException{
    public DuplicateResourceException(String resourceName, String field, String value){
        super(
                String.format("%s with %s '%s' already exists", resourceName, field, value),
                "DUPLICATE_RESOURCE",
                HttpStatus.CONFLICT
        );
        withDetail("resource", resourceName);
        withDetail("field", field);
        withDetail("value", value);
    }

    public static DuplicateResourceException duplicateEmail(String email){
        return new DuplicateResourceException("User", "email", email);
    }

    public static DuplicateResourceException duplicateUserEmail(String email) {
        return (DuplicateResourceException) new DuplicateResourceException("User", "email", email)
                .withDetail("suggestion", "Please use a different email address");
    }

    public static DuplicateResourceException duplicateGoogleId(String googleId){
        return (DuplicateResourceException) new DuplicateResourceException("User", "googleId", googleId)
                .withDetail("suggestion", "Please use a different email address");
    }

    public static DuplicateResourceException



}