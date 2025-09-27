package com.linkups.domain.exception;
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

    public static ResourceNotFoundException availabilityNotFound(Long availabilityId) {
        return (ResourceNotFoundException) new ResourceNotFoundException("Availability", availabilityId)
                .withDetail("suggestion", "Please verify the availability ID and try again");
    }

    public static ResourceNotFoundException circleNotFound(Long circleId) {
        return new ResourceNotFoundException("Circle", circleId);
    }

    public static ResourceNotFoundException circleMemberNotFound(Long userId, Long circleId) {
        return new ResourceNotFoundException("CircleMember", userId + " in circle " + circleId);
    }

    public static ResourceNotFoundException passwordResetTokenNotFound(String token) {
        return (ResourceNotFoundException) new ResourceNotFoundException(
                "Password reset token not found or invalid")
                .withDetail("suggestion", "The password reset link may be invalid or expired. Please request a new password reset.");
    }

    public static ResourceNotFoundException passwordResetTokenExpired(String token) {
        return (ResourceNotFoundException) new ResourceNotFoundException(
                "Password reset token has expired")
                .withDetail("suggestion", "Password reset links expire after 30 minutes. Please request a new password reset.");
    }

    public static ResourceNotFoundException passwordResetTokenUsed(String token) {
        return (ResourceNotFoundException) new ResourceNotFoundException(
                "Password reset token has already been used")
                .withDetail("suggestion", "Each password reset link can only be used once. Please request a new password reset if needed.");
    }

    public static ResourceNotFoundException passwordResetUserNotFound(String email) {
        return (ResourceNotFoundException) new ResourceNotFoundException(
                "No account found for password reset")
                .withDetail("email", email)
                .withDetail("suggestion", "Please check your email address or register for a new account");
    }

    public static ResourceNotFoundException chatRoomNotFound(Long roomId) {
        return (ResourceNotFoundException) new ResourceNotFoundException("Chat room", roomId)
                .withDetail("suggestion", "The chat room may have been deleted or you may not have access");
    }

    public static ResourceNotFoundException chatParticipantNotFound(Long userId, Long roomId) {
        return (ResourceNotFoundException) new ResourceNotFoundException(
                "User is not a participant in this chat room")
                .withDetail("userId", userId)
                .withDetail("roomId", roomId)
                .withDetail("suggestion", "Make sure the user is added to the chat room first");
    }

    public static ResourceNotFoundException messageNotFound(Long messageId) {
        return (ResourceNotFoundException) new ResourceNotFoundException("Message", messageId)
                .withDetail("suggestion", "The message may have been deleted");
    }

    public static ResourceNotFoundException noMessagesFound(Long roomId) {
        return (ResourceNotFoundException) new ResourceNotFoundException(
                "No messages found in this chat room")
                .withDetail("roomId", roomId)
                .withDetail("suggestion", "Start a conversation by sending the first message");
    }

    public static ResourceNotFoundException privateRoomNotFound(Long userId1, Long userId2) {
        return (ResourceNotFoundException) new ResourceNotFoundException(
                "Private chat room not found between these users")
                .withDetail("user1Id", userId1)
                .withDetail("user2Id", userId2)
                .withDetail("suggestion", "A private chat will be created when you send the first message");
    }

}