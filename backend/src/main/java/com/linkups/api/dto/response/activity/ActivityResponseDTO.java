package com.linkups.api.dto.response.activity;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Activity Response DTO
 *
 * Response DTO for activity data.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActivityResponseDTO {

    /**
     * Activity ID
     */
    private Long id;

    /**
     * Activity type
     */
    private String type;

    /**
     * User ID who performed the activity
     */
    private Long userId;

    /**
     * User name who performed the activity
     */
    private String userName;

    /**
     * Activity timestamp
     */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime timestamp;

    /**
     * Activity data (JSON string)
     */
    private String data;

    /**
     * Priority level (1-5)
     */
    private Integer priority;

    /**
     * Visibility level
     */
    private String visibility;

    /**
     * Whether the activity is active
     */
    private Boolean isActive;

    /**
     * Related entity ID
     */
    private Long relatedEntityId;

    /**
     * Related entity type
     */
    private String relatedEntityType;

    /**
     * When the activity was created
     */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;

    // Helper methods

    /**
     * Check if the activity is high priority (4-5)
     */
    public boolean isHighPriority() {
        return priority != null && priority >= 4;
    }

    /**
     * Check if the activity is public
     */
    public boolean isPublic() {
        return "public".equals(visibility);
    }

    /**
     * Check if the activity is for friends only
     */
    public boolean isFriendsOnly() {
        return "friends".equals(visibility);
    }
}