package com.linkups.api.dto.response.circle;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.util.List;

/**
 * Circle List Response DTO
 *
 * Data Transfer Object for returning a list of circles with
 * aggregate statistics about the user's membership roles.
 *
 * <p>This DTO includes:</p>
 * <ul>
 *   <li>List of circles with full details</li>
 *   <li>Total count of circles</li>
 *   <li>Breakdown by user's role (owner, admin, member)</li>
 * </ul>
 *
 * <p>Used for:</p>
 * <ul>
 *   <li>GET /api/circles/user/{userId} - Get all circles for a user</li>
 *   <li>GET /api/circles/search - Search results</li>
 * </ul>
 *
 * @see CircleResponse
 * @see CircleSummaryDTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CircleListResponse {

    /**
     * List of circles
     */
    private List<CircleResponse> circles;

    /**
     * Total number of circles in the list
     */
    private Integer totalCount;

    /**
     * Number of circles where user is OWNER
     */
    private Integer circlesAsOwner;

    /**
     * Number of circles where user is ADMIN
     */
    private Integer circlesAsAdmin;

    /**
     * Number of circles where user is MEMBER
     */
    private Integer circlesAsMember;

    // Helper methods

    /**
     * Check if the list is empty
     */
    public boolean isEmpty() {
        return circles == null || circles.isEmpty();
    }

    /**
     * Get the actual size of the circles list
     * <p>May differ from totalCount if pagination is applied</p>
     */
    public int getActualSize() {
        return circles != null ? circles.size() : 0;
    }
}