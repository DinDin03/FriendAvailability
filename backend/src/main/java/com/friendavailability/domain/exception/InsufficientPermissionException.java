package com.friendavailability.domain.exception;

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

}