package com.friendavailability.domain.exception;

import jakarta.persistence.criteria.CriteriaBuilder;
import org.springframework.http.HttpStatus;

public class InvalidOperationException extends BusinessException {

    public InvalidOperationException(String message) {
        super(message, "INVALID_OPERATION", HttpStatus.BAD_REQUEST);
    }


    public static InvalidOperationException cannotAddSelfAsFriend(Long userId) {
        return (InvalidOperationException) new InvalidOperationException("You cannot add yourself as a friend")
                .withDetail("userId", userId);
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

    public static InvalidOperationException friendShipAlreadyExists(Long userId1, Long userId2){
        return (InvalidOperationException) new InvalidOperationException(
                "Already friends with the user")
                .withDetail("user1", userId1)
                .withDetail("user2", userId2)
                .withDetail("suggestion", "Users are already friends or have a pending request");
    }

    public static InvalidOperationException circleAtMaxCapacity() {
        return new InvalidOperationException("Circle has reached its maximum member limit");
    }

    public static InvalidOperationException mustBeFriendsFirst() {
        return new InvalidOperationException("Users must be friends first");
    }

    public static InvalidOperationException userAlreadyMember() {
        return new InvalidOperationException("User is already a member of the circle");
    }

    public static InvalidOperationException cannotRemoveLastOwner(){
        return new InvalidOperationException("Can not remove last owner from the circle, transfer ownership or delete the circle");
    }

    public static InvalidOperationException cannotDirectlyAssignOwner(){
        return new InvalidOperationException("Cannot directly assign owner role, must transfer ownership");
    }

    public static InvalidOperationException cannotDemoteLastOwner(){
        return new InvalidOperationException("Cannot demote last owner of the circle");
    }

    public static InvalidOperationException cannotCreatePrivateRoomWithSelf() {
        return (InvalidOperationException) new InvalidOperationException(
                "Cannot create a private chat room with yourself")
                .withDetail("suggestion", "Please select a different user to chat with");
    }

    public static InvalidOperationException cannotAddSelfToGroup() {
        return (InvalidOperationException) new InvalidOperationException(
                "You are already a member of this group")
                .withDetail("suggestion", "You don't need to add yourself to the group");
    }

    public static InvalidOperationException cannotRemoveSelfFromGroup() {
        return (InvalidOperationException) new InvalidOperationException(
                "Use the leave group function to exit this chat")
                .withDetail("suggestion", "Use the 'Leave Group' option instead");
    }

    public static InvalidOperationException cannotPromoteSelfInGroup() {
        return (InvalidOperationException) new InvalidOperationException(
                "You cannot promote yourself in a group chat")
                .withDetail("suggestion", "Ask another admin to promote you if needed");
    }

    public static InvalidOperationException groupChatTooFewParticipants() {
        return (InvalidOperationException) new InvalidOperationException(
                "Group chats require at least 2 participants")
                .withDetail("minimumParticipants", 2)
                .withDetail("suggestion", "Add at least one other person to create a group chat");
    }

    public static InvalidOperationException duplicateParticipantInGroup(Long userId) {
        return (InvalidOperationException) new InvalidOperationException(
                "User is already a participant in this group chat")
                .withDetail("userId", userId)
                .withDetail("suggestion", "This user is already in the group");
    }

    public static InvalidOperationException cannotSendEmptyMessage() {
        return (InvalidOperationException) new InvalidOperationException(
                "Cannot send an empty message")
                .withDetail("suggestion", "Please enter some text before sending");
    }






}
