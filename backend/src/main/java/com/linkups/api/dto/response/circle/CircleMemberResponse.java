package com.linkups.api.dto.response.circle;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Circle Member Response DTO
 *
 * Data Transfer Object representing a member of a circle with their
 * role, permissions, and user information.
 *
 * <p>This DTO includes:</p>
 * <ul>
 *   <li>Membership information (ID, role, join date)</li>
 *   <li>User information (ID, name, email)</li>
 *   <li>Invitation details (who invited them)</li>
 *   <li>Permissions (whether they can be promoted/removed)</li>
 * </ul>
 *
 * <p>Used for:</p>
 * <ul>
 *   <li>GET /api/circles/{circleId}/members - List of members</li>
 *   <li>POST /api/circles/{circleId}/members - After adding a member</li>
 *   <li>PUT /api/circles/{circleId}/members/{userId}/role - After role update</li>
 * </ul>
 *
 * @see com.linkups.domain.entity.CircleMember
 * @see com.linkups.api.mapper.CircleMapper
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CircleMemberResponse {

    /**
     * Unique identifier of the membership record
     */
    private Long membershipId;

    /**
     * ID of the user who is a member
     */
    private Long userId;

    /**
     * Name of the user
     */
    private String userName;

    /**
     * Email of the user
     */
    private String userEmail;

    /**
     * Role of the member in the circle
     * <p>Values: "OWNER", "ADMIN", "MEMBER"</p>
     */
    private String role;

    /**
     * Human-readable display name of the role
     * <p>Examples: "Owner", "Administrator", "Member"</p>
     */
    private String roleDisplayName;

    /**
     * When the member joined the circle
     */
    private LocalDateTime joinedAt;

    /**
     * ID of the user who invited this member
     * <p>null if self-joined or owner (created the circle)</p>
     */
    private Long invitedBy;

    /**
     * Name of the user who invited this member
     * <p>null if self-joined or owner</p>
     */
    private String invitedByName;

    /**
     * Whether the requesting user can promote/demote this member
     */
    private Boolean canPromote;

    /**
     * Whether the requesting user can remove this member from the circle
     */
    private Boolean canRemove;

    // Helper methods

    /**
     * Check if this member is the owner
     */
    public boolean isOwner() {
        return "OWNER".equals(role);
    }

    /**
     * Check if this member is an admin
     */
    public boolean isAdmin() {
        return "ADMIN".equals(role);
    }

    /**
     * Check if this member is a regular member
     */
    public boolean isMember() {
        return "MEMBER".equals(role);
    }

    /**
     * Get formatted join date as string
     */
    public String getFormattedJoinedAt() {
        return joinedAt != null ? joinedAt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")) : null;
    }

    /**
     * Check if member was invited by someone
     */
    public boolean wasInvited() {
        return invitedBy != null;
    }
}