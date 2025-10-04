package com.linkups.api.dto.response.circle;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.util.List;

/**
 * Circle Member List Response DTO
 *
 * Data Transfer Object for returning a list of circle members with
 * aggregate statistics about role distribution.
 *
 * <p>This DTO includes:</p>
 * <ul>
 *   <li>List of members with their details</li>
 *   <li>Total member count</li>
 *   <li>Breakdown by role (owner, admin, member)</li>
 * </ul>
 *
 * <p>Used for:</p>
 * <ul>
 *   <li>GET /api/circles/{circleId}/members - Get all members of a circle</li>
 * </ul>
 *
 * @see CircleMemberResponse
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CircleMemberListResponse {

    /**
     * List of members in the circle
     */
    private List<CircleMemberResponse> members;

    /**
     * Total number of members
     */
    private Integer totalMembers;

    /**
     * Number of members with OWNER role
     */
    private Integer ownerCount;

    /**
     * Number of members with ADMIN role
     */
    private Integer adminCount;

    /**
     * Number of members with MEMBER role
     */
    private Integer memberCount;

    // Helper methods

    /**
     * Check if the list is empty
     */
    public boolean isEmpty() {
        return members == null || members.isEmpty();
    }

    /**
     * Get the actual size of the members list
     */
    public int getActualSize() {
        return members != null ? members.size() : 0;
    }

    /**
     * Check if circle has any owners
     */
    public boolean hasOwners() {
        return ownerCount != null && ownerCount > 0;
    }

    /**
     * Check if circle has any admins
     */
    public boolean hasAdmins() {
        return adminCount != null && adminCount > 0;
    }
}