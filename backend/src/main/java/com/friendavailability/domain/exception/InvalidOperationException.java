package com.friendavailability.domain.exception;
import org.springframework.http.HttpStatus;

public class InvalidOperationException extends BusinessException {
    public InvalidOperationException(String message){
        super(
            message,
            "INVALID_OPERATION"
            HttpStatus.BAD_REQUEST
        )
    }

    public static InvalidOperationException cannotAddSelfAsFriend() {
        return new InvalidOperationException("You cannot add yourself as a friend");
    }

    public static InvalidOperationException circleAtCapacity(Long circleId, int maxMembers) {
        return new InvalidOperationException("Circle has reached maximum capacity")
            .withDetail("circleId", circleId)
            .withDetail("maxMembers", maxMembers);
    }

    public static InvalidOperationException alreadyMember(Long userId, Long circleId) {
        return new InvalidOperationException("User is already a member of this circle")
            .withDetail("userId", userId)
            .withDetail("circleId", circleId);
    }
}
