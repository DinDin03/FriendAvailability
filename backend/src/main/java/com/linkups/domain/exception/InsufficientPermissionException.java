package com.linkups.domain.exception;

import org.springframework.http.HttpStatus;

public class InsufficientPermissionException extends BusinessException {
    
    public InsufficientPermissionException(String action, String resource) {
        super(
            String.format("You don't have permission to %s this %s", action, resource),
            "INSUFFICIENT_PERMISSION",
            HttpStatus.FORBIDDEN  
        );
        withDetail("action", action);
        withDetail("resource", resource);
    }
    
    public InsufficientPermissionException(String message) {
        super(message, "INSUFFICIENT_PERMISSION", HttpStatus.FORBIDDEN);
    }

    public static InsufficientPermissionException notCircleOwner(Long circleId) {
        return (InsufficientPermissionException) new InsufficientPermissionException("Only the circle owner can perform this action")
            .withDetail("circleId", circleId);
    }

    public static InsufficientPermissionException notFriends(Long userId1, Long userId2) {
        return (InsufficientPermissionException) new InsufficientPermissionException("Users must be friends to perform this action")
            .withDetail("userId1", userId1)
            .withDetail("userId2", userId2);
    }

    public static InsufficientPermissionException onlyRecipientsCanRespond(String action){
        return (InsufficientPermissionException) new InsufficientPermissionException(String.format("Only recipients can %s this friend request", action))
                .withDetail("action", action)
                .withDetail("suggestion", "Only the person who received the request can respond to it");
    }

    public static InsufficientPermissionException onlyOwnerCanDelete() {
        return new InsufficientPermissionException("Only the circle owner can delete the circle");
    }

    public static InsufficientPermissionException onlyAdminCanUpdate() {
        return new InsufficientPermissionException("Only circle owners and admins can update circle details");
    }

    public static InsufficientPermissionException onlyAdminCanAddMembers() {
        return new InsufficientPermissionException("Only the owner and admins can add users");
    }

    public static InsufficientPermissionException mustBeMemberToView() {
        return new InsufficientPermissionException("You must be a member of the circle to view its details");
    }

    public static InsufficientPermissionException onlyOwnerCanTransferOwnership() {
        return new InsufficientPermissionException("You must be the owner of the circle to perform a change of ownership");
    }

    public static InsufficientPermissionException onlyAdminCanRemoveMembers(){
        return new InsufficientPermissionException("You must be an admin to remove members");
    }

    public static InsufficientPermissionException onlyOwnerCanUpdateRoles(){
        return new InsufficientPermissionException("You must be the owner to update user roles");
    }

    public static InsufficientPermissionException cannotAccessChatRoom(Long roomId) {
        return (InsufficientPermissionException) new InsufficientPermissionException(
                "You don't have permission to access this chat room")
                .withDetail("roomId", roomId)
                .withDetail("suggestion", "Make sure you are a participant in this chat room");
    }

    public static InsufficientPermissionException cannotSendMessageToRoom(Long senderId, Long roomId) {
        return (InsufficientPermissionException) new InsufficientPermissionException(
                "You don't have permission to send messages in this chat room")
                .withDetail("senderId", senderId)
                .withDetail("roomId", roomId)
                .withDetail("suggestion", "Contact a group admin to get permission");
    }

    public static InsufficientPermissionException cannotAddUsersToGroup() {
        return (InsufficientPermissionException) new InsufficientPermissionException(
                "Only group admins can add users to this group chat")
                .withDetail("suggestion", "Ask a group admin to add this user");
    }

    public static InsufficientPermissionException cannotRemoveUsersFromGroup() {
        return (InsufficientPermissionException) new InsufficientPermissionException(
                "Only group admins can remove users from this group chat")
                .withDetail("suggestion", "Ask a group admin to remove this user");
    }

    public static InsufficientPermissionException cannotPromoteUser() {
        return (InsufficientPermissionException) new InsufficientPermissionException(
                "Only group owners can promote users to admin")
                .withDetail("suggestion", "Ask the group owner to promote this user");
    }

    public static InsufficientPermissionException cannotViewChatParticipants(Long roomId) {
        return (InsufficientPermissionException) new InsufficientPermissionException(
                "You don't have permission to view participants in this chat room")
                .withDetail("roomId", roomId)
                .withDetail("suggestion", "Make sure you are a participant in this chat room");
    }

    public static InsufficientPermissionException cannotDeleteMessage(Long userId) {
        return new InsufficientPermissionException("Only message sender or room admin can delete messages");
    }

    public static InsufficientPermissionException notActiveParticipant(Long userId, Long roomId) {
        return new InsufficientPermissionException("User is not an active participant in this room");
    }

    public static InsufficientPermissionException cannotModifyActivity(Long userId, Long activityId) {
        return (InsufficientPermissionException) new InsufficientPermissionException(
                "You don't have permission to modify this activity")
                .withDetail("userId", userId)
                .withDetail("activityId", activityId)
                .withDetail("suggestion", "Only the activity owner can modify it");
    }

    public static InsufficientPermissionException cannotViewActivity(Long userId, Long activityId) {
        return (InsufficientPermissionException) new InsufficientPermissionException(
                "You don't have permission to view this activity")
                .withDetail("userId", userId)
                .withDetail("activityId", activityId)
                .withDetail("suggestion", "This activity may be private or not visible to you");
    }
}