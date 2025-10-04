package com.linkups.api.dto.response.circle;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Circle Response DTO
 *
 * Data Transfer Object representing a circle with full details including
 * the requesting user's role and permissions within the circle.
 *
 * <p>This DTO includes:</p>
 * <ul>
 *   <li>Basic circle information (name, description, settings)</li>
 *   <li>Membership statistics (current count, max capacity)</li>
 *   <li>User-specific information (role, permissions)</li>
 *   <li>Timestamps and metadata</li>
 * </ul>
 *
 * <p>Used for:</p>
 * <ul>
 *   <li>GET /api/circles/{circleId} - Single circle details</li>
 *   <li>POST /api/circles - After creating a circle</li>
 *   <li>PUT /api/circles/{circleId} - After updating a circle</li>
 * </ul>
 *
 * @see com.linkups.domain.entity.Circle
 * @see com.linkups.api.mapper.CircleMapper
 * @see CircleSummaryDTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CircleResponse {

    /**
     * Unique identifier of the circle
     */
    private Long id;

    /**
     * Name of the circle
     */
    private String name;

    /**
     * Optional description of the circle's purpose
     */
    private String description;

    /**
     * User ID of the circle creator/owner
     */
    private Long createdBy;

    /**
     * Name of the user who created the circle
     */
    private String createdByName;

    /**
     * Maximum number of members allowed
     * <p>null = unlimited members</p>
     */
    private Integer maxMembers;

    /**
     * Current number of active members in the circle
     */
    private Integer currentMemberCount;

    /**
     * Hex color code for circle display
     * <p>Default: "#0052CC"</p>
     */
    private String circleColor;

    /**
     * When the circle was created
     */
    private LocalDateTime createdAt;

    /**
     * When the circle was last updated
     */
    private LocalDateTime updatedAt;

    /**
     * Requesting user's role in this circle
     * <p>Values: "OWNER", "ADMIN", "MEMBER"</p>
     */
    private String userRole;

    /**
     * Whether the requesting user can manage members (add/remove)
     */
    private Boolean canManageMembers;

    /**
     * Whether the requesting user can modify circle settings
     */
    private Boolean canModifyCircle;

    /**
     * Whether the requesting user can delete the circle
     */
    private Boolean canDeleteCircle;

    /**
     * Whether the circle is active (not soft-deleted)
     */
    private Boolean isActive;

    /**
     * Whether the circle has reached its maximum capacity
     */
    private Boolean isAtMaxCapacity;

    // Helper methods

    /**
     * Check if the requesting user is the owner
     */
    public boolean isOwner() {
        return "OWNER".equals(userRole);
    }

    /**
     * Check if the requesting user is an admin or owner
     */
    public boolean isAdminOrOwner() {
        return "OWNER".equals(userRole) || "ADMIN".equals(userRole);
    }

    /**
     * Get formatted creation date as string
     */
    public String getFormattedCreatedAt() {
        return createdAt != null ? createdAt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")) : null;
    }

    /**
     * Get capacity percentage (0-100)
     * <p>Returns null if maxMembers is null (unlimited)</p>
     */
    public Integer getCapacityPercentage() {
        if (maxMembers == null || maxMembers == 0 || currentMemberCount == null) {
            return null;
        }
        return (int) ((currentMemberCount * 100.0) / maxMembers);
    }

    /**
     * Get remaining slots available
     * <p>Returns null if unlimited capacity</p>
     */
    public Integer getRemainingSlots() {
        if (maxMembers == null || currentMemberCount == null) {
            return null;
        }
        return maxMembers - currentMemberCount;
    }

    /**
     * Check if circle has capacity restrictions
     */
    public boolean hasMaxMembers() {
        return maxMembers != null && maxMembers > 0;
    }
}