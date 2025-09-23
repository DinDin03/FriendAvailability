package com.friendavailability.domain.exception;

import jakarta.persistence.criteria.CriteriaBuilder;
import org.springframework.http.HttpStatus;

public class InvalidOperationException extends BusinessException {

    public InvalidOperationException(String message) {
        super(message, "INVALID_OPERATION", HttpStatus.BAD_REQUEST);
    }


    public static InvalidOperationException cannotAddSelfAsFriend() {
        return new InvalidOperationException("You cannot add yourself as a friend");
    }

    public static InvalidOperationException circleAtCapacity(Long circleId, int maxMembers) {
        return (InvalidOperationException) new InvalidOperationException("Circle has reached maximum capacity")
                .withDetail("circleId", circleId)
                .withDetail("maxMembers", maxMembers);
    }

    public static InvalidOperationException alreadyMember(Long userId, Long circleId) {
        return (InvalidOperationException) new InvalidOperationException("User is already a member of this circle")
                .withDetail("userId", userId)
                .withDetail("circleId", circleId);
    }


    public static InvalidOperationException googleUserUsingPassword(String email) {
        return (InvalidOperationException) new InvalidOperationException(
                "This account uses Google sign-in. Please use the 'Sign in with Google' button instead.")
                .withDetail("email", email)
                .withDetail("authProvider", "Google")
                .withDetail("suggestion", "Look for the 'Sign in with Google' button on the login page");
    }


    public static InvalidOperationException accountTemporarilyLocked(String email, int lockoutMinutes) {
        return (InvalidOperationException) new InvalidOperationException(
                String.format("Account temporarily locked due to multiple failed login attempts. Please try again in %d minutes.", lockoutMinutes))
                .withDetail("email", email)
                .withDetail("lockoutMinutes", lockoutMinutes)
                .withDetail("suggestion", "Wait for the lockout period to expire or reset your password");
    }


    public static InvalidOperationException insufficientAccountLevel(String requiredLevel, String currentLevel) {
        return (InvalidOperationException) new InvalidOperationException(
                String.format("This action requires %s account level. Your current level is %s.", requiredLevel, currentLevel))
                .withDetail("requiredLevel", requiredLevel)
                .withDetail("currentLevel", currentLevel)
                .withDetail("suggestion", "Upgrade your account to access this feature");
    }

    public static InvalidOperationException friendRequestNotPending(String currentStatus){
        return (InvalidOperationException) new InvalidOperationException(
                "Friend request is not pending")
                .withDetail("currentStatus", currentStatus)
                .withDetail("suggestion", "Only pending requests can be accepted or deleted");
    }




}
