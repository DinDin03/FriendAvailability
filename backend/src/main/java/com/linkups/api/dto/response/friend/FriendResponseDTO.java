package com.linkups.api.dto.response.friend;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Friend Response DTO
 *
 * Response DTO for friend/friendship data with status information.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FriendResponseDTO {

    /**
     * Friendship ID
     */
    private Long id;

    /**
     * User ID who initiated the friend request
     */
    private Long userId;

    /**
     * Friend user ID
     */
    private Long friendId;

    /**
     * Friend's name
     */
    private String friendName;

    /**
     * Friend's email
     */
    private String friendEmail;

    /**
     * Friendship status
     * Values: "PENDING", "ACCEPTED", "REJECTED"
     */
    private String status;

    /**
     * When the friendship was created
     */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;

    /**
     * When the friendship was last updated
     */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime updatedAt;

    // Helper methods

    /**
     * Check if friendship is pending
     */
    public boolean isPending() {
        return "PENDING".equals(status);
    }

    /**
     * Check if friendship is accepted
     */
    public boolean isAccepted() {
        return "ACCEPTED".equals(status);
    }

    /**
     * Check if friendship is rejected
     */
    public boolean isRejected() {
        return "REJECTED".equals(status);
    }
}