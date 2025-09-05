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

    public static DuplicateResourceException duplicateCircleName(String circleName){
        return new DuplicateResourceException("Circle", "name", circleName);
    }

    // I will create more duplicate exceptions in the future

}