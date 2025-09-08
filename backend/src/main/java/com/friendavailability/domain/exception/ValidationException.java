package com.friendavailability.domain.exception;

import org.springframework.http.HttpStatus;
import java.time.LocalDateTime;

public class ValidationException extends BusinessException {

    public ValidationException(String message) {
        super(message, "VALIDATION_FAILED", HttpStatus.BAD_REQUEST);
    }

    public ValidationException(String field, String message) {
        super(message, "VALIDATION_FAILED", HttpStatus.BAD_REQUEST);
        withDetail("field", field);
    }

    public static ValidationException requiredEmail() {
        return new ValidationException("email", "Email is required");
    }

    public static ValidationException invalidEmail(String email) {
        return (ValidationException) new ValidationException("email", "Please provide a valid email address")
                .withDetail("providedValue", email);
    }

    public static ValidationException requiredPassword() {
        return new ValidationException("password", "Password is required");
    }

    public static ValidationException passwordTooShort(int minLength) {
        return (ValidationException) new ValidationException("password",
                String.format("Password must be at least %d characters long", minLength))
                .withDetail("minLength", minLength);
    }

    public static ValidationException passwordTooLong(int maxLength) {
        return (ValidationException) new ValidationException("password",
                String.format("Password cannot exceed %d characters", maxLength))
                .withDetail("maxLength", maxLength);
    }

    public static ValidationException requiredName() {
        return new ValidationException("name", "Name is required");
    }

    public static ValidationException nameTooShort(int minLength) {
        return (ValidationException) new ValidationException("name",
                String.format("Name must be at least %d characters long", minLength))
                .withDetail("minLength", minLength);
    }

    public static ValidationException nameTooLong(int maxLength, int actualLength) {
        return (ValidationException) new ValidationException("name",
                String.format("Name cannot exceed %d characters", maxLength))
                .withDetail("maxLength", maxLength)
                .withDetail("actualLength", actualLength);
    }

    public static ValidationException invalidFieldValue(String fieldName, String reason) {
        return new ValidationException(fieldName, reason);
    }

    public static ValidationException invalidDateRange(LocalDateTime start, LocalDateTime end) {
        return (ValidationException) new ValidationException("Start time must be before end time")
                .withDetail("startTime", start.toString())
                .withDetail("endTime", end.toString());
    }

    public static ValidationException invalidPassword(String reason) {
        return new ValidationException("password", "Invalid password: " + reason);
    }
}