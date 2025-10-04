package com.linkups.api.dto.response.circle;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * Circle Summary DTO
 *
 * Lightweight Data Transfer Object for circle list views.
 * Contains only essential circle information without full details.
 *
 * <p>This DTO is optimized for:</p>
 * <ul>
 *   <li>List views with many circles</li>
 *   <li>Search results</li>
 *   <li>Navigation menus/sidebars</li>
 *   <li>Quick overviews</li>
 * </ul>
 *
 * <p>Comparison with CircleResponse:</p>
 * <ul>
 *   <li>CircleSummaryDTO - Minimal fields for lists (faster, smaller payload)</li>
 *   <li>CircleResponse - Complete details for single circle view</li>
 * </ul>
 *
 * @see CircleResponse
 * @see CircleListResponse
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CircleSummaryDTO {

    /**
     * Unique identifier of the circle
     */
    private Long id;

    /**
     * Name of the circle
     */
    private String name;

    /**
     * Hex color code for circle display
     * <p>Default: "#0052CC"</p>
     */
    private String circleColor;

    /**
     * Current number of active members
     */
    private Integer currentMemberCount;

    /**
     * Maximum number of members allowed
     * <p>null = unlimited members</p>
     */
    private Integer maxMembers;

    /**
     * Whether the circle has reached its maximum capacity
     */
    private Boolean isAtMaxCapacity;

    /**
     * User's role in this circle
     * <p>Values: "OWNER", "ADMIN", "MEMBER"</p>
     */
    private String userRole;

    // Helper methods

    /**
     * Check if the user is the owner
     */
    public boolean isOwner() {
        return "OWNER".equals(userRole);
    }

    /**
     * Check if the user is an admin or owner
     */
    public boolean isAdminOrOwner() {
        return "OWNER".equals(userRole) || "ADMIN".equals(userRole);
    }

    /**
     * Check if circle has capacity restrictions
     */
    public boolean hasMaxMembers() {
        return maxMembers != null && maxMembers > 0;
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
}