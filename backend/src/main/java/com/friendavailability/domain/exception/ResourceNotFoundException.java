package com.friendavailability.domain.exception;

import org.apache.catalina.valves.rewrite.ResolverImpl;
import org.springframework.http.HttpStatus;

public class ResourceNotFoundException extends BusinessException {

    public ResourceNotFoundException(String resourceName, Long id) {
        super(
                String.format("%s with id %d not found", resourceName, id),
                "RESOURCE_NOT_FOUND",
                HttpStatus.NOT_FOUND
        );
        withDetail("resource", resourceName);
        withDetail("id", id);
    }

    public ResourceNotFoundException(String resourceName, String identifier) {
        super(
                String.format("%s with id %s not found", resourceName, identifier),
                "RESOURCE_NOT_FOUND",
                HttpStatus.NOT_FOUND
        );
        withDetail("resource", resourceName);
        withDetail("identifier", identifier);
    }

    public ResourceNotFoundException(String message) {
        super(message, "RESOURCE_NOT_FOUND", HttpStatus.NOT_FOUND);
    }

    public ResourceNotFoundException(String message, Throwable cause) {
        super(message, "RESOURCE_NOT_FOUND", HttpStatus.NOT_FOUND, cause);
    }

    public static ResourceNotFoundException userEmailNotFound(String email) {
        return (ResourceNotFoundException) new ResourceNotFoundException("User", email)
                .withDetail("message", "No account found with this email address")
                .withDetail("suggestion", "Please check your email address or register for a new account");
    }

    public static ResourceNotFoundException noAuthenticatedUser() {
        return (ResourceNotFoundException) new ResourceNotFoundException(
                "No authenticated user found. Please log in to continue.")
                .withDetail("suggestion", "Please log in and try again");
    }

    public static ResourceNotFoundException userGoogleIdNotFound(String googleId) {
        return (ResourceNotFoundException) new ResourceNotFoundException("User", googleId)
                .withDetail("authProvider", "Google")
                .withDetail("suggestion", "This Google account is not linked to any user account");
    }

    public static ResourceNotFoundException userNotFound(Long userId) {
        return (ResourceNotFoundException) new ResourceNotFoundException("User", userId)
                .withDetail("suggestion", "Please verify the user ID and try again");
    }

    public static ResourceNotFoundException friendRequestNotFound(Long friendshipId){
        return (ResourceNotFoundException) new ResourceNotFoundException("Friendship not found")
                .withDetail("friendshipId", friendshipId)
                .withDetail("suggestion", "Try sending the friend request again");
    }

}