package com.linkups.domain.exception;

import org.springframework.http.HttpStatus;


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


    public static ValidationException loginWithUnverifiedEmail(String email) {
        return (ValidationException) new ValidationException("email",
                "Please verify your email before logging in. Check your inbox for the verification link.")
                .withDetail("email", email)
                .withDetail("suggestion", "Check your spam folder if you don't see the verification email");
    }
    public static ValidationException loginWithDisabledAccount(String email) {
        return (ValidationException) new ValidationException("account",
                "Your account has been disabled. Please contact support for assistance.")
                .withDetail("email", email)
                .withDetail("suggestion", "Contact support to reactivate your account");
    }


    public static ValidationException incorrectCredentials() {
        return new ValidationException("credentials",
                "Invalid email or password. Please check your credentials and try again.");
    }

    public static ValidationException passwordTooWeak() {
        return (ValidationException) new ValidationException("password",
                "Password is too weak. Please choose a stronger password.")
                .withDetail("requirements", "Include uppercase, lowercase, numbers, and special characters")
                .withDetail("suggestion", "Try a passphrase with mixed characters");
    }


    public static ValidationException invalidFieldValue(String fieldName, String reason) {
        return new ValidationException(fieldName, reason);
    }

    public static ValidationException invalidPassword(String reason) {
        return new ValidationException("password", "Invalid password: " + reason);
    }
    
    public static ValidationException invalidUserId(Long userId) {
        return (ValidationException) new ValidationException("userId", "Invalid user ID provided")
                .withDetail("providedValue", userId);
    }

    public static ValidationException noUserDataToUpdate() {
        return new ValidationException("request", "No valid user data provided for update");
    }

    public static ValidationException invalidTimeRange() {
        return (ValidationException) new ValidationException("time",
                "Start time must be before end time")
                .withDetail("suggestion", "Please ensure start time is earlier than end time");
    }

    public static ValidationException eventInPast() {
        return (ValidationException) new ValidationException("startTime",
                "Cannot create events in the past")
                .withDetail("suggestion", "Please choose a future date and time");
    }

    public static ValidationException invalidAllDayEvent() {
        return (ValidationException) new ValidationException("allDay",
                "All day events must be full days")
                .withDetail("suggestion", "All day events should span complete days");
    }

    public static ValidationException negativeReminder() {
        return (ValidationException) new ValidationException("reminderMinutes",
                "Reminder cannot be negative")
                .withDetail("suggestion", "Please provide a positive number of minutes for reminders");
    }

    public static ValidationException circleNameRequired() {
        return new ValidationException("name", "Circle name cannot be empty");
    }

    public static ValidationException circleNameTooShort(int minLength) {
        return new ValidationException("name", "Circle name must be at least " + minLength + " characters long");
    }

    public static ValidationException circleNameTooLong(int maxLength) {
        return new ValidationException("name", "Circle name cannot exceed " + maxLength + " characters");
    }

    public static ValidationException circleDescriptionTooLong(int maxLength) {
        return new ValidationException("description", "Circle description cannot exceed " + maxLength + " characters");
    }

    public static ValidationException passwordResetRequiredPassword() {
        return new ValidationException("password", "New password is required");
    }

    public static ValidationException passwordResetTooShort(int minLength) {
        return (ValidationException) new ValidationException("password",
                String.format("Password must be at least %d characters long", minLength))
                .withDetail("minLength", minLength)
                .withDetail("suggestion", "Please choose a longer password");
    }

    public static ValidationException passwordResetInvalidToken() {
        return (ValidationException) new ValidationException("token",
                "Password reset token is required")
                .withDetail("suggestion", "Please use the link from your password reset email");
    }

    public static ValidationException passwordResetEmailRequired() {
        return new ValidationException("email", "Email address is required for password reset");
    }

    public static ValidationException passwordResetEmailUnverified(String email) {
        return (ValidationException) new ValidationException("email",
                "Password reset is only available for verified email addresses")
                .withDetail("email", email)
                .withDetail("suggestion", "Please verify your email first, then request password reset");
    }

    public static ValidationException passwordResetRateLimited(int remainingMinutes) {
        return (ValidationException) new ValidationException("rate_limit",
                "Too many password reset attempts. Please try again later.")
                .withDetail("waitTimeMinutes", remainingMinutes)
                .withDetail("suggestion", "Please wait before requesting another password reset");
    }

    public static ValidationException messageContentRequired() {
        return new ValidationException("content", "Message content cannot be empty");
    }

    public static ValidationException messageContentTooLong(int maxLength) {
        return (ValidationException) new ValidationException("content",
                String.format("Message content cannot exceed %d characters", maxLength))
                .withDetail("maxLength", maxLength)
                .withDetail("suggestion", "Please shorten your message");
    }

    public static ValidationException chatRoomNameRequired() {
        return new ValidationException("name", "Chat room name is required");
    }

    public static ValidationException chatRoomNameTooShort(int minLength) {
        return (ValidationException) new ValidationException("name",
                String.format("Chat room name must be at least %d characters long", minLength))
                .withDetail("minLength", minLength);
    }

    public static ValidationException chatRoomNameTooLong(int maxLength) {
        return (ValidationException) new ValidationException("name",
                String.format("Chat room name cannot exceed %d characters", maxLength))
                .withDetail("maxLength", maxLength);
    }

    public static ValidationException invalidChatRoomId(Long roomId) {
        return (ValidationException) new ValidationException("roomId", "Invalid chat room ID provided")
                .withDetail("providedValue", roomId)
                .withDetail("suggestion", "Please verify the chat room ID");
    }

    public static ValidationException invalidMessageSearchTerm() {
        return (ValidationException) new ValidationException("searchTerm", "Search term must be at least 2 characters")
                .withDetail("suggestion", "Please enter a longer search term");
    }

    public static ValidationException searchTermRequired() {
        return new ValidationException("searchTerm", "Search term cannot be empty");
    }

    public static ValidationException searchTermTooShort(int minLength) {
        return new ValidationException("searchTerm",
                String.format("Search term must be at least %d characters", minLength));
    }



}