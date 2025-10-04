package com.linkups.api.mapper;

import com.linkups.api.dto.response.circle.*;
import com.linkups.domain.entity.Circle;
import com.linkups.domain.entity.CircleMember;
import com.linkups.domain.entity.User;
import com.linkups.domain.entity.enums.CircleRole;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Circle Mapper
 *
 * Utility class for converting between Circle/CircleMember domain entities and DTOs.
 * Provides null-safe mapping methods to transform entities into Response DTOs,
 * while preventing circular reference issues and protecting sensitive data.
 *
 * <p>This mapper ensures:</p>
 * <ul>
 *   <li>Separation between internal domain model and external API contracts</li>
 *   <li>Circular reference safety (Circle ← members → Circle)</li>
 *   <li>Null safety for all lazy-loaded relationships</li>
 *   <li>User-specific permission calculations</li>
 *   <li>Aggregate statistics for list responses</li>
 * </ul>
 *
 * <p>Key Design Patterns:</p>
 * <ul>
 *   <li>Never include full Circle in CircleMemberResponse (only ID/name)</li>
 *   <li>Never include full member lists in CircleResponse (only counts)</li>
 *   <li>Always require CircleMember for user-specific fields</li>
 *   <li>Use summary DTOs for lists, full DTOs for details</li>
 * </ul>
 *
 * @see Circle
 * @see CircleMember
 * @see CircleResponse
 * @see CircleMemberResponse
 * @see CircleSummaryDTO
 */
public class CircleMapper {

    /**
     * Private constructor to prevent instantiation of utility class
     */
    private CircleMapper() {
        throw new UnsupportedOperationException("CircleMapper is a utility class and cannot be instantiated");
    }

    // ========== CIRCLE TO RESPONSE ==========

    /**
     * Convert Circle entity to CircleResponse DTO with user-specific information
     *
     * This method creates a full circle response including the requesting user's
     * role and permissions. The memberCount is fetched from the repository if needed.
     *
     * @param circle Circle entity to convert (can be null)
     * @param userMembership CircleMember record for the requesting user (can be null)
     * @return CircleResponse DTO, or null if input circle is null
     */
    public static CircleResponse toResponse(Circle circle, CircleMember userMembership) {
        if (circle == null) {
            return null;
        }

        // Calculate member count safely
        Integer memberCount = circle.getMembers() != null ? circle.getMembers().size() : 0;

        CircleResponse.CircleResponseBuilder builder = CircleResponse.builder()
                .id(circle.getId())
                .name(circle.getName())
                .description(circle.getDescription())
                .createdBy(circle.getCreatedBy())
                .maxMembers(circle.getMaxMembers())
                .currentMemberCount(memberCount)
                .circleColor(circle.getCircleColor() != null ? circle.getCircleColor() : "#0052CC")
                .createdAt(circle.getCreatedAt())
                .updatedAt(circle.getUpdatedAt())
                .isActive(circle.getIsActive() != null ? circle.getIsActive() : true)
                .isAtMaxCapacity(circle.isAtMaxCapacity());

        // Add user-specific fields if membership provided
        if (userMembership != null) {
            builder.userRole(userMembership.getRole() != null ? userMembership.getRole().name() : "MEMBER")
                    .canManageMembers(userMembership.canManageMembers())
                    .canModifyCircle(userMembership.canModifyCircle())
                    .canDeleteCircle(userMembership.isOwner());
        }

        return builder.build();
    }

    /**
     * Convert Circle entity to CircleResponse with explicit member count
     *
     * Use this variant when you already have the member count from a repository query
     * to avoid additional database calls.
     *
     * @param circle Circle entity to convert (can be null)
     * @param userMembership CircleMember record for the requesting user (can be null)
     * @param memberCount Pre-calculated member count
     * @return CircleResponse DTO, or null if input circle is null
     */
    public static CircleResponse toResponse(Circle circle, CircleMember userMembership, Integer memberCount) {
        if (circle == null) {
            return null;
        }

        CircleResponse.CircleResponseBuilder builder = CircleResponse.builder()
                .id(circle.getId())
                .name(circle.getName())
                .description(circle.getDescription())
                .createdBy(circle.getCreatedBy())
                .maxMembers(circle.getMaxMembers())
                .currentMemberCount(memberCount != null ? memberCount : 0)
                .circleColor(circle.getCircleColor() != null ? circle.getCircleColor() : "#0052CC")
                .createdAt(circle.getCreatedAt())
                .updatedAt(circle.getUpdatedAt())
                .isActive(circle.getIsActive() != null ? circle.getIsActive() : true)
                .isAtMaxCapacity(circle.hasMaxMembers() && memberCount != null && memberCount >= circle.getMaxMembers());

        // Add user-specific fields if membership provided
        if (userMembership != null) {
            builder.userRole(userMembership.getRole() != null ? userMembership.getRole().name() : "MEMBER")
                    .canManageMembers(userMembership.canManageMembers())
                    .canModifyCircle(userMembership.canModifyCircle())
                    .canDeleteCircle(userMembership.isOwner());
        }

        return builder.build();
    }

    /**
     * Convert list of Circle entities to list of CircleResponse DTOs
     *
     * Note: This method cannot populate user-specific fields (role, permissions)
     * because it doesn't have access to CircleMember records. Use toCircleListResponse
     * instead if you need complete responses with user information.
     *
     * @param circles List of Circle entities to convert (can be null or contain nulls)
     * @return List of CircleResponse DTOs (never null, but may be empty)
     */
    public static List<CircleResponse> toResponseList(List<Circle> circles) {
        if (circles == null || circles.isEmpty()) {
            return Collections.emptyList();
        }

        return circles.stream()
                .filter(circle -> circle != null)
                .map(circle -> toResponse(circle, null))
                .collect(Collectors.toList());
    }

    // ========== CIRCLE TO SUMMARY ==========

    /**
     * Convert Circle entity to CircleSummaryDTO for lightweight list views
     *
     * Use this for search results and navigation menus where minimal data is needed.
     *
     * @param circle Circle entity to convert (can be null)
     * @param userMembership CircleMember record for the requesting user (can be null)
     * @return CircleSummaryDTO, or null if input circle is null
     */
    public static CircleSummaryDTO toSummary(Circle circle, CircleMember userMembership) {
        if (circle == null) {
            return null;
        }

        Integer memberCount = circle.getMembers() != null ? circle.getMembers().size() : 0;

        CircleSummaryDTO.CircleSummaryDTOBuilder builder = CircleSummaryDTO.builder()
                .id(circle.getId())
                .name(circle.getName())
                .circleColor(circle.getCircleColor() != null ? circle.getCircleColor() : "#0052CC")
                .currentMemberCount(memberCount)
                .maxMembers(circle.getMaxMembers())
                .isAtMaxCapacity(circle.isAtMaxCapacity());

        if (userMembership != null) {
            builder.userRole(userMembership.getRole() != null ? userMembership.getRole().name() : "MEMBER");
        }

        return builder.build();
    }

    /**
     * Convert Circle entity to CircleSummaryDTO with explicit member count
     *
     * @param circle Circle entity to convert (can be null)
     * @param userMembership CircleMember record for the requesting user (can be null)
     * @param memberCount Pre-calculated member count
     * @return CircleSummaryDTO, or null if input circle is null
     */
    public static CircleSummaryDTO toSummary(Circle circle, CircleMember userMembership, Integer memberCount) {
        if (circle == null) {
            return null;
        }

        CircleSummaryDTO.CircleSummaryDTOBuilder builder = CircleSummaryDTO.builder()
                .id(circle.getId())
                .name(circle.getName())
                .circleColor(circle.getCircleColor() != null ? circle.getCircleColor() : "#0052CC")
                .currentMemberCount(memberCount != null ? memberCount : 0)
                .maxMembers(circle.getMaxMembers())
                .isAtMaxCapacity(circle.hasMaxMembers() && memberCount != null && memberCount >= circle.getMaxMembers());

        if (userMembership != null) {
            builder.userRole(userMembership.getRole() != null ? userMembership.getRole().name() : "MEMBER");
        }

        return builder.build();
    }

    // ========== CIRCLE MEMBER TO RESPONSE ==========

    /**
     * Convert CircleMember entity to CircleMemberResponse DTO
     *
     * Extracts user information from the lazy-loaded User relationship.
     * Handles null users gracefully.
     *
     * @param member CircleMember entity to convert (can be null)
     * @return CircleMemberResponse DTO, or null if input member is null
     */
    public static CircleMemberResponse toMemberResponse(CircleMember member) {
        if (member == null) {
            return null;
        }

        // Extract user information safely
        User user = member.getUser();
        Long userId = user != null ? user.getId() : member.getUserId();
        String userName = user != null ? user.getName() : "Unknown User";
        String userEmail = user != null ? user.getEmail() : null;

        return CircleMemberResponse.builder()
                .membershipId(member.getId())
                .userId(userId)
                .userName(userName)
                .userEmail(userEmail)
                .role(member.getRole() != null ? member.getRole().name() : "MEMBER")
                .roleDisplayName(member.getRole() != null ? member.getRole().getDisplayName() : "Member")
                .joinedAt(member.getJoinedAt())
                .invitedBy(member.getInvitedBy())
                .canPromote(false) // Will be set by service layer based on requesting user
                .canRemove(false)  // Will be set by service layer based on requesting user
                .build();
    }

    /**
     * Convert CircleMember entity to CircleMemberResponse with permission flags
     *
     * Use this variant when you want to calculate permissions based on the requesting user.
     *
     * @param member CircleMember entity to convert (can be null)
     * @param requestingUserMembership CircleMember record for the requesting user
     * @return CircleMemberResponse DTO, or null if input member is null
     */
    public static CircleMemberResponse toMemberResponse(CircleMember member, CircleMember requestingUserMembership) {
        CircleMemberResponse response = toMemberResponse(member);
        if (response == null || requestingUserMembership == null) {
            return response;
        }

        // Calculate permissions
        boolean canManageRoles = requestingUserMembership.isOwner();
        boolean canManageMembers = requestingUserMembership.canManageMembers();

        // Can only promote/demote if you're owner and target is not owner
        response.setCanPromote(canManageRoles && !member.isOwner());

        // Can remove if you can manage members AND (target is not owner OR you're owner)
        response.setCanRemove(canManageMembers && (!member.isOwner() || requestingUserMembership.isOwner()));

        return response;
    }

    /**
     * Convert list of CircleMember entities to list of CircleMemberResponse DTOs
     *
     * @param members List of CircleMember entities to convert (can be null or contain nulls)
     * @return List of CircleMemberResponse DTOs (never null, but may be empty)
     */
    public static List<CircleMemberResponse> toMemberResponseList(List<CircleMember> members) {
        if (members == null || members.isEmpty()) {
            return Collections.emptyList();
        }

        return members.stream()
                .filter(member -> member != null)
                .map(CircleMapper::toMemberResponse)
                .collect(Collectors.toList());
    }

    /**
     * Convert list of CircleMember entities with permission calculations
     *
     * @param members List of CircleMember entities to convert
     * @param requestingUserMembership CircleMember record for the requesting user
     * @return List of CircleMemberResponse DTOs with permissions
     */
    public static List<CircleMemberResponse> toMemberResponseList(List<CircleMember> members, CircleMember requestingUserMembership) {
        if (members == null || members.isEmpty()) {
            return Collections.emptyList();
        }

        return members.stream()
                .filter(member -> member != null)
                .map(member -> toMemberResponse(member, requestingUserMembership))
                .collect(Collectors.toList());
    }

    // ========== LIST RESPONSE BUILDERS ==========

    /**
     * Build CircleListResponse from list of circles with user-specific information
     *
     * This method requires a map of circle IDs to CircleMember records to properly
     * populate user roles and permissions for each circle.
     *
     * @param circles List of Circle entities
     * @param userMemberships Map of circle ID to CircleMember (requesting user's membership)
     * @return CircleListResponse with statistics
     */
    public static CircleListResponse toCircleListResponse(List<Circle> circles, Map<Long, CircleMember> userMemberships) {
        if (circles == null || circles.isEmpty()) {
            return CircleListResponse.builder()
                    .circles(Collections.emptyList())
                    .totalCount(0)
                    .circlesAsOwner(0)
                    .circlesAsAdmin(0)
                    .circlesAsMember(0)
                    .build();
        }

        // Convert circles to responses with user-specific info
        List<CircleResponse> responses = circles.stream()
                .filter(circle -> circle != null)
                .map(circle -> {
                    CircleMember membership = userMemberships != null ? userMemberships.get(circle.getId()) : null;
                    return toResponse(circle, membership);
                })
                .collect(Collectors.toList());

        // Calculate role statistics
        long ownerCount = responses.stream().filter(r -> "OWNER".equals(r.getUserRole())).count();
        long adminCount = responses.stream().filter(r -> "ADMIN".equals(r.getUserRole())).count();
        long memberCount = responses.stream().filter(r -> "MEMBER".equals(r.getUserRole())).count();

        return CircleListResponse.builder()
                .circles(responses)
                .totalCount(responses.size())
                .circlesAsOwner((int) ownerCount)
                .circlesAsAdmin((int) adminCount)
                .circlesAsMember((int) memberCount)
                .build();
    }

    /**
     * Build CircleMemberListResponse from list of members with role statistics
     *
     * @param members List of CircleMember entities
     * @return CircleMemberListResponse with role counts
     */
    public static CircleMemberListResponse toMemberListResponse(List<CircleMember> members) {
        if (members == null || members.isEmpty()) {
            return CircleMemberListResponse.builder()
                    .members(Collections.emptyList())
                    .totalMembers(0)
                    .ownerCount(0)
                    .adminCount(0)
                    .memberCount(0)
                    .build();
        }

        List<CircleMemberResponse> responses = toMemberResponseList(members);

        // Calculate role statistics
        long ownerCount = members.stream().filter(m -> m.getRole() == CircleRole.OWNER).count();
        long adminCount = members.stream().filter(m -> m.getRole() == CircleRole.ADMIN).count();
        long memberCount = members.stream().filter(m -> m.getRole() == CircleRole.MEMBER).count();

        return CircleMemberListResponse.builder()
                .members(responses)
                .totalMembers(responses.size())
                .ownerCount((int) ownerCount)
                .adminCount((int) adminCount)
                .memberCount((int) memberCount)
                .build();
    }

    /**
     * Build CircleMemberListResponse with permission calculations
     *
     * @param members List of CircleMember entities
     * @param requestingUserMembership CircleMember record for the requesting user
     * @return CircleMemberListResponse with permissions and role counts
     */
    public static CircleMemberListResponse toMemberListResponse(List<CircleMember> members, CircleMember requestingUserMembership) {
        if (members == null || members.isEmpty()) {
            return CircleMemberListResponse.builder()
                    .members(Collections.emptyList())
                    .totalMembers(0)
                    .ownerCount(0)
                    .adminCount(0)
                    .memberCount(0)
                    .build();
        }

        List<CircleMemberResponse> responses = toMemberResponseList(members, requestingUserMembership);

        // Calculate role statistics
        long ownerCount = members.stream().filter(m -> m.getRole() == CircleRole.OWNER).count();
        long adminCount = members.stream().filter(m -> m.getRole() == CircleRole.ADMIN).count();
        long memberCount = members.stream().filter(m -> m.getRole() == CircleRole.MEMBER).count();

        return CircleMemberListResponse.builder()
                .members(responses)
                .totalMembers(responses.size())
                .ownerCount((int) ownerCount)
                .adminCount((int) adminCount)
                .memberCount((int) memberCount)
                .build();
    }
}