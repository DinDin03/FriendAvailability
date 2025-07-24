package com.friendavailability.controller;

import com.friendavailability.dto.circle.*;
import com.friendavailability.model.Circle;
import com.friendavailability.model.CircleMember;
import com.friendavailability.model.CircleRole;
import com.friendavailability.service.CircleService;
import com.friendavailability.service.UserService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/circles")
@CrossOrigin(origins = "*")
public class CircleController {

    private final CircleService circleService;

    public CircleController(CircleService circleService, UserService userService) {
        this.circleService = circleService;
    }

    @PostMapping
    public ResponseEntity<?> createCircle(@RequestBody CreateCircleRequest request, @RequestParam Long userId) {
        try {
            Circle circle = circleService.createCircle(userId, request.getName(), request.getDescription(),
                    request.getMaxMembers());

            return ResponseEntity.ok(Map.of(
                    "message", "Circle created successfully",
                    "circle", Map.of(
                            "id", circle.getId(),
                            "name", circle.getName(),
                            "description", circle.getDescription(),
                            "createdBy", circle.getCreatedBy(),
                            "maxMembers", circle.getMaxMembers(),
                            "circleColor", circle.getCircleColor(),
                            "createdAt", circle.getCreatedAt(),
                            "memberCount", 1)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", e.getMessage(),
                    "errorCode", "CREATE_CIRCLE_FAILED"));
        }
    }

    @PutMapping("/{circleId}")
    public ResponseEntity<?> updateCircle(@PathVariable Long circleId, @RequestBody UpdateCircleRequest request,
            @RequestParam Long userId) {
        try {
            Circle circle = circleService.updateCircle(circleId, request.getName(), request.getDescription(), userId);
            return ResponseEntity.ok(Map.of(
                    "message", "Circle updated successfully",
                    "circle", Map.of(
                            "id", circle.getId(),
                            "name", circle.getName(),
                            "description", circle.getDescription(),
                            "updatedAt", circle.getUpdatedAt())));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", e.getMessage(),
                    "errorCode", "UPDATE_CIRCLE_FAILED"));
        }
    }

    @DeleteMapping("/{circleId}")
    public ResponseEntity<?> deleteCircle(@PathVariable Long circleId, @RequestParam Long userId) {
        try {
            boolean deleted = circleService.deleteCircle(circleId, userId);

            if (deleted) {
                return ResponseEntity.ok(Map.of(
                        "message", "Circle deleted successfully"));
            } else {
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "Failed to delete circle",
                        "errorCode", "DELETE_CIRCLE_FAILED"));
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", e.getMessage(),
                    "errorCode", "DELETE_CIRCLE_FAILED"));
        }
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<?> getUserCircles(@PathVariable Long userId) {
        try {
            List<Circle> userCircles = circleService.getCirclesForUser(userId);
            List<Circle> createdCircles = circleService.getCirclesCreatedByUser(userId);

            return ResponseEntity.ok(Map.of(
                    "circles", userCircles,
                    "totalCircles", userCircles.size(),
                    "circlesAsOwner", createdCircles.size(),
                    "circlesAsMember", userCircles.size() - createdCircles.size()));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", e.getMessage(),
                    "errorCode", "GET_CIRCLES_FAILED"));
        }
    }

    @GetMapping("/{circleId}")
    public ResponseEntity<?> getCircle(@PathVariable Long circleId, @RequestParam Long userId) {
        try {
            boolean userAccess = circleService.canUserAccessCircle(circleId, userId);

            if (!userAccess) {
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "You are not a member of this circle ",
                        "errorCode", "ACCESS_DENIED"));
            }

            Circle circle = circleService.getCircle(circleId);

            long memberCount = circleService.getMemberCountForCircle(circleId);

            CircleMember userMembership = circleService.getUserMembershipInCircle(circleId, userId);

            Map<String, Object> response = new HashMap<>();
            response.put("id", circle.getId());
            response.put("name", circle.getName());
            response.put("description", circle.getDescription());
            response.put("createdBy", circle.getCreatedBy());
            response.put("maxMembers", circle.getMaxMembers());
            response.put("currentMemberCount", memberCount);
            response.put("circleColor", circle.getCircleColor());
            response.put("createdAt", circle.getCreatedAt());
            response.put("updatedAt", circle.getUpdatedAt());
            response.put("userRole", userMembership.getRole().name());
            response.put("canManageMembers", userMembership.canManageMembers());
            response.put("canModifyCircle", userMembership.canModifyCircle());
            response.put("isAtMaxCapacity", circle.isAtMaxCapacity());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", e.getMessage(),
                    "errorCode", "GET_CIRCLE_FAILED"));
        }
    }

    @GetMapping("/{circleId}/members")
    public ResponseEntity<?> getCircleMembers(@PathVariable Long circleId, @RequestParam Long userId) {
        try {
            boolean hasAccess = circleService.canUserAccessCircle(circleId, userId);

            if (!hasAccess) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                        "error", "you are not a member of this circle",
                        "errorCode", "ACCESS_DENIED"));
            }

            List<CircleMember> members = circleService.getCircleMembers(circleId, userId);

            return ResponseEntity.ok(Map.of(
                    "members", members,
                    "totalMembers", members.size(),
                    "OwnerCount", members.stream().filter(m -> m.getRole().name().equals("OWNER")).count(),
                    "adminCount", members.stream().filter(m -> m.getRole().name().equals("ADMIN")).count(),
                    "memberCount", members.stream().filter(m -> m.getRole().name().equals("MEMBER")).count(),
                    "message", "Circle members retrieved successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", e.getMessage(),
                    "errorCode", "GET_MEMBERS_FAILED"));
        }
    }

    @PostMapping("/{circleId}/members")
    public ResponseEntity<?> addMemberToCircle(@PathVariable Long circleId, @RequestParam Long requestingUserId,
            @RequestBody AddMemberToCircleRequest request) {

        try {
            CircleMember membership = circleService.addMemberToCircle(circleId, request.getUserId(), requestingUserId);

            return ResponseEntity.ok(Map.of(
                    "message", "Member added successfully",
                    "membership", Map.of(
                            "id", membership.getId(),
                            "userId", membership.getUserId(),
                            "userName", membership.getUserName(),
                            "role", membership.getRole().name(),
                            "joinedAt", membership.getJoinedAt())));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", e.getMessage(),
                    "errorCode", "ADD_MEMBER_FAILED"));
        }
    }

    @DeleteMapping("/{circleId}/members/{userId}")
    public ResponseEntity<?> removeMemberFromCircle(@PathVariable Long circleId, @PathVariable Long userId,
            @RequestParam Long requestingUserId) {
        try {
            boolean deleted = circleService.removeMemberFromCircle(circleId, userId, requestingUserId);

            if (deleted) {
                return ResponseEntity.ok(Map.of(
                        "message", "Member removed successfully"));
            } else {
                return ResponseEntity.badRequest().body(Map.of(
                        "erorr", "Failed to remove member",
                        "errorCode", "REMOVE_FAILED"));
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", e.getMessage(),
                    "errorCode", "REMOVE_MEMBER_FAILED"));
        }
    }

    @PutMapping("/{circleId}/members/{userId}/role")
    public ResponseEntity<?> updateMemberRole(@PathVariable Long circleId,
            @PathVariable Long userId,
            @RequestBody UpdateMemberRoleRequest request,
            @RequestParam Long requestingUserId) {
        try {
            CircleRole newRole = CircleRole.valueOf(request.getNewRole().toUpperCase());
            CircleMember updatedMember = circleService.updateMemberRole(circleId, userId, newRole, requestingUserId);

            return ResponseEntity.ok(Map.of(
                    "message", "Member role updated successfully",
                    "member", Map.of(
                            "userId", updatedMember.getUserId(),
                            "userName", updatedMember.getUserName(),
                            "newRole", updatedMember.getRole().name(),
                            "roleDisplayName", updatedMember.getRoleDisplayName())));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Invalid role: " + request.getNewRole(),
                    "errorCode", "INVALID_ROLE"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", e.getMessage(),
                    "errorCode", "UPDATE_ROLE_FAILED"));
        }
    }

    @PostMapping("/{circleId}/transfer-ownership")
    public ResponseEntity<?> transferOwnership(@PathVariable Long circleId,
            @RequestBody TransferOwnershipRequest request,
            @RequestParam Long currentOwnerId) {
        try {
            boolean transferred = circleService.transferOwnership(
                    circleId,
                    request.getNewOwnerId(),
                    currentOwnerId);

            if (transferred) {
                return ResponseEntity.ok(Map.of(
                        "message", "Ownership transferred successfully"));
            } else {
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "Failed to transfer ownership",
                        "errorCode", "TRANSFER_FAILED"));
            }

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", e.getMessage(),
                    "errorCode", "TRANSFER_OWNERSHIP_FAILED"));
        }
    }

    @GetMapping("/search")
    public ResponseEntity<?> searchCircles(@RequestParam String searchTerm, @RequestParam Long userId) {
        try {
            List<Circle> allResults = circleService.searchCircles(searchTerm);
            List<Circle> userCircles = circleService.getCirclesForUser(userId);

            List<Circle> filteredResults = allResults.stream()
                    .filter(circle -> userCircles.stream()
                            .anyMatch(userCircle -> userCircle.getId().equals(circle.getId())))
                    .toList();

            return ResponseEntity.ok(Map.of(
                    "searchResults", filteredResults,
                    "totalResults", filteredResults.size(),
                    "searchTerm", searchTerm,
                    "message", "Search completed successfully"));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", e.getMessage(),
                    "errorCode", "SEARCH_FAILED"));
        }

    }
}
