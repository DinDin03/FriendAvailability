package com.friendavailability.domain.exception;

import org.springframework.http.HttpStatus;
import java.util.Map;
import java.util.HashMap;

import java.time.LocalDateTime;

public class ValidationException extends BusinessException{
    
    public ValidationException(String message){
        super(
            message,
            "VALIDATION_FAILED",
            HttpStatus.BAD_REQUEST
        );
    }

    public ValidationException(String field, String message) {
        super(
            message,
            "VALIDATION_FAILED",
            HttpStatus.BAD_REQUEST
        );
        withDetail("field", field);
    }

    public static ValidationException invalidDateRange(LocalDateTime start, LocalDateTime end){
        return new ValidationException("Start time must be before end time")
        .withDetail("startTime", start.toString())
        .withDetail("endTime", end.toString())
    }

    public static ValidationException invalidPassword(String reason) {
    return new ValidationException("password", "Invalid password: " + reason);
}


}
