package com.linkups.api.dto.response.userstatus;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * User Status Response DTO
 *
 * Response DTO for user status information.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserStatusResponseDTO {

    /**
     * Status ID
     */
    private Long id;

    /**
     * User ID
     */
    private Long userId;

    /**
     * User name
     */
    private String userName;

    /**
     * Current status
     * Values: "ONLINE", "AWAY", "BUSY", "OFFLINE"
     */
    private String status;

    /**
     * Last seen timestamp
     */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime lastSeen;

    /**
     * Current activity description
     */
    private String currentActivity;

    /**
     * When the status was last updated
     */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime updatedAt;

    // Helper methods

    /**
     * Check if user is online
     */
    public boolean isOnline() {
        return "ONLINE".equals(status);
    }

    /**
     * Check if user is away
     */
    public boolean isAway() {
        return "AWAY".equals(status);
    }

    /**
     * Check if user is busy
     */
    public boolean isBusy() {
        return "BUSY".equals(status);
    }

    /**
     * Check if user is offline
     */
    public boolean isOffline() {
        return "OFFLINE".equals(status);
    }
}