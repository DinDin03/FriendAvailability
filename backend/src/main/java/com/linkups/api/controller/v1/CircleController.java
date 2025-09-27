package com.linkups.api.controller.v1;

import com.linkups.api.dto.request.circle.*;
import com.linkups.domain.entity.Circle;
import com.linkups.domain.entity.CircleMember;
import com.linkups.domain.entity.enums.CircleRole;
import com.linkups.domain.service.CircleService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/circles")
@CrossOrigin(origins = "*")
@Slf4j
public class CircleController {

    private final CircleService circleService;

    public CircleController(CircleService circleService) {
        this.circleService = circleService;
        log.info("CircleController initialized successfully");
    }

    @PostMapping
    public ResponseEntity<Circle> createCircle(@Valid @RequestBody CreateCircleRequest request, @RequestParam Long userId) {
        log.info("Creating circle '{}' for user {}", request.getName(), userId);

        Circle circle = circleService.createCircle(userId, request.getName(), request.getDescription(), request.getMaxMembers());

        log.info("Circle '{}' created successfully with ID {}", request.getName(), circle.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(circle);
    }

    @PutMapping("/{circleId}")
    public ResponseEntity<Circle> updateCircle(@PathVariable Long circleId,
                                               @Valid @RequestBody UpdateCircleRequest request,
                                               @RequestParam Long userId) {
        log.info("Updating circle {} by user {}", circleId, userId);

        Circle circle = circleService.updateCircle(circleId, request.getName(), request.getDescription(), userId);

        log.info("Circle {} updated successfully", circleId);
        return ResponseEntity.ok(circle);
    }

    @DeleteMapping("/{circleId}")
    public ResponseEntity<Map<String, String>> deleteCircle(@PathVariable Long circleId, @RequestParam Long userId) {
        log.info("Deleting circle {} by user {}", circleId, userId);

        circleService.deleteCircle(circleId, userId);

        Map<String, String> response = Map.of("message", "Circle deleted successfully");

        log.info("Circle {} deleted successfully", circleId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<Map<String, Object>> getUserCircles(@PathVariable Long userId) {
        log.info("Getting circles for user {}", userId);

        List<Circle> userCircles = circleService.getCirclesForUser(userId);
        List<Circle> createdCircles = circleService.getCirclesCreatedByUser(userId);

        Map<String, Object> response = Map.of(
                "circles", userCircles,
                "totalCircles", userCircles.size(),
                "circlesAsOwner", createdCircles.size(),
                "circlesAsMember", userCircles.size() - createdCircles.size()
        );

        log.info("Retrieved {} circles for user {}", userCircles.size(), userId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{circleId}")
    public ResponseEntity<Map<String, Object>> getCircle(@PathVariable Long circleId, @RequestParam Long userId) {
        log.info("Getting circle {} for user {}", circleId, userId);

        Circle circle = circleService.getCircle(circleId);
        long memberCount = circleService.getMemberCountForCircle(circleId);
        CircleMember userMembership = circleService.getUserMembershipInCircle(circleId, userId);

        Map<String, Object> response = new java.util.HashMap<>();
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

        log.info("Retrieved circle {} details for user {}", circleId, userId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{circleId}/members")
    public ResponseEntity<Map<String, Object>> getCircleMembers(@PathVariable Long circleId, @RequestParam Long userId) {
        log.info("Getting members of circle {} for user {}", circleId, userId);

        List<CircleMember> members = circleService.getCircleMembers(circleId, userId);

        Map<String, Object> response = Map.of(
                "members", members,
                "totalMembers", members.size(),
                "ownerCount", members.stream().filter(m -> m.getRole().name().equals("OWNER")).count(),
                "adminCount", members.stream().filter(m -> m.getRole().name().equals("ADMIN")).count(),
                "memberCount", members.stream().filter(m -> m.getRole().name().equals("MEMBER")).count()
        );

        log.info("Retrieved {} members for circle {}", members.size(), circleId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{circleId}/members")
    public ResponseEntity<CircleMember> addMemberToCircle(@PathVariable Long circleId,
                                                          @RequestParam Long requestingUserId,
                                                          @Valid @RequestBody AddMemberToCircleRequest request) {
        log.info("Adding user {} to circle {} by user {}", request.getUserId(), circleId, requestingUserId);

        CircleMember membership = circleService.addMemberToCircle(circleId, request.getUserId(), requestingUserId);

        log.info("User {} added to circle {} successfully", request.getUserId(), circleId);
        return ResponseEntity.status(HttpStatus.CREATED).body(membership);
    }

    @DeleteMapping("/{circleId}/members/{userId}")
    public ResponseEntity<Map<String, String>> removeMemberFromCircle(@PathVariable Long circleId,
                                                                      @PathVariable Long userId,
                                                                      @RequestParam Long requestingUserId) {
        log.info("Removing user {} from circle {} by user {}", userId, circleId, requestingUserId);

        circleService.removeMemberFromCircle(circleId, userId, requestingUserId);

        Map<String, String> response = Map.of("message", "Member removed successfully");

        log.info("User {} removed from circle {} successfully", userId, circleId);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{circleId}/members/{userId}/role")
    public ResponseEntity<CircleMember> updateMemberRole(@PathVariable Long circleId,
                                                         @PathVariable Long userId,
                                                         @Valid @RequestBody UpdateMemberRoleRequest request,
                                                         @RequestParam Long requestingUserId) {
        log.info("Updating role of user {} in circle {} to {} by user {}", userId, circleId, request.getNewRole(), requestingUserId);

        CircleRole newRole = CircleRole.valueOf(request.getNewRole().toUpperCase());
        CircleMember updatedMember = circleService.updateMemberRole(circleId, userId, newRole, requestingUserId);

        log.info("Role of user {} in circle {} updated to {} successfully", userId, circleId, newRole);
        return ResponseEntity.ok(updatedMember);
    }

    @PostMapping("/{circleId}/transfer-ownership")
    public ResponseEntity<Map<String, String>> transferOwnership(@PathVariable Long circleId,
                                                                 @Valid @RequestBody TransferOwnershipRequest request,
                                                                 @RequestParam Long currentOwnerId) {
        log.info("Transferring ownership of circle {} from user {} to user {}", circleId, currentOwnerId, request.getNewOwnerId());

        circleService.transferOwnership(circleId, request.getNewOwnerId(), currentOwnerId);

        Map<String, String> response = Map.of("message", "Ownership transferred successfully");

        log.info("Ownership of circle {} transferred successfully", circleId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/search")
    public ResponseEntity<Map<String, Object>> searchCircles(@RequestParam String searchTerm, @RequestParam Long userId) {
        log.info("Searching circles with term '{}' for user {}", searchTerm, userId);

        List<Circle> allResults = circleService.searchCircles(searchTerm);
        List<Circle> userCircles = circleService.getCirclesForUser(userId);

        List<Circle> filteredResults = allResults.stream()
                .filter(circle -> userCircles.stream()
                        .anyMatch(userCircle -> userCircle.getId().equals(circle.getId())))
                .toList();

        Map<String, Object> response = Map.of(
                "searchResults", filteredResults,
                "totalResults", filteredResults.size(),
                "searchTerm", searchTerm
        );

        log.info("Found {} circles matching search term '{}' for user {}", filteredResults.size(), searchTerm, userId);
        return ResponseEntity.ok(response);
    }
}