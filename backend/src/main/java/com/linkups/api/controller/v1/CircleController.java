package com.linkups.api.controller.v1;

import com.linkups.api.dto.request.circle.*;
import com.linkups.api.dto.response.circle.*;
import com.linkups.api.mapper.CircleMapper;
import com.linkups.domain.entity.Circle;
import com.linkups.domain.entity.CircleMember;
import com.linkups.domain.entity.enums.CircleRole;
import com.linkups.domain.service.CircleService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
    public ResponseEntity<CircleResponse> createCircle(@Valid @RequestBody CreateCircleRequest request, @RequestParam Long userId) {
        log.info("Creating circle '{}' for user {}", request.getName(), userId);

        Circle circle = circleService.createCircle(request, userId);
        CircleMember userMembership = circleService.getUserMembershipInCircle(circle.getId(), userId);

        log.info("Circle '{}' created successfully with ID {}", request.getName(), circle.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(CircleMapper.toResponse(circle, userMembership));
    }

    @PutMapping("/{circleId}")
    public ResponseEntity<CircleResponse> updateCircle(@PathVariable Long circleId,
                                                       @Valid @RequestBody UpdateCircleRequest request,
                                                       @RequestParam Long userId) {
        log.info("Updating circle {} by user {}", circleId, userId);

        Circle circle = circleService.updateCircle(circleId, request, userId);
        CircleMember userMembership = circleService.getUserMembershipInCircle(circleId, userId);
        long memberCount = circleService.getMemberCountForCircle(circleId);

        log.info("Circle {} updated successfully", circleId);
        return ResponseEntity.ok(CircleMapper.toResponse(circle, userMembership, (int) memberCount));
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
    public ResponseEntity<CircleListResponse> getUserCircles(@PathVariable Long userId) {
        log.info("Getting circles for user {}", userId);

        List<Circle> userCircles = circleService.getCirclesForUser(userId);

        // Build a map of circle ID to user's membership in that circle
        Map<Long, CircleMember> userMemberships = userCircles.stream()
                .collect(Collectors.toMap(
                        Circle::getId,
                        circle -> circleService.getUserMembershipInCircle(circle.getId(), userId)
                ));

        CircleListResponse response = CircleMapper.toCircleListResponse(userCircles, userMemberships);

        log.info("Retrieved {} circles for user {}", userCircles.size(), userId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{circleId}")
    public ResponseEntity<CircleResponse> getCircle(@PathVariable Long circleId, @RequestParam Long userId) {
        log.info("Getting circle {} for user {}", circleId, userId);

        Circle circle = circleService.getCircle(circleId);
        CircleMember userMembership = circleService.getUserMembershipInCircle(circleId, userId);
        long memberCount = circleService.getMemberCountForCircle(circleId);

        log.info("Retrieved circle {} details for user {}", circleId, userId);
        return ResponseEntity.ok(CircleMapper.toResponse(circle, userMembership, (int) memberCount));
    }

    @GetMapping("/{circleId}/members")
    public ResponseEntity<CircleMemberListResponse> getCircleMembers(@PathVariable Long circleId, @RequestParam Long userId) {
        log.info("Getting members of circle {} for user {}", circleId, userId);

        List<CircleMember> members = circleService.getCircleMembers(circleId, userId);
        CircleMember requestingUserMembership = circleService.getUserMembershipInCircle(circleId, userId);

        CircleMemberListResponse response = CircleMapper.toMemberListResponse(members, requestingUserMembership);

        log.info("Retrieved {} members for circle {}", members.size(), circleId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{circleId}/members")
    public ResponseEntity<CircleMemberResponse> addMemberToCircle(@PathVariable Long circleId,
                                                                  @RequestParam Long requestingUserId,
                                                                  @Valid @RequestBody AddMemberToCircleRequest request) {
        log.info("Adding user {} to circle {} by user {}", request.getUserId(), circleId, requestingUserId);

        CircleMember membership = circleService.addMemberToCircle(circleId, request.getUserId(), requestingUserId);
        CircleMember requestingUserMembership = circleService.getUserMembershipInCircle(circleId, requestingUserId);

        log.info("User {} added to circle {} successfully", request.getUserId(), circleId);
        return ResponseEntity.status(HttpStatus.CREATED).body(CircleMapper.toMemberResponse(membership, requestingUserMembership));
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
    public ResponseEntity<CircleMemberResponse> updateMemberRole(@PathVariable Long circleId,
                                                                 @PathVariable Long userId,
                                                                 @Valid @RequestBody UpdateMemberRoleRequest request,
                                                                 @RequestParam Long requestingUserId) {
        log.info("Updating role of user {} in circle {} to {} by user {}", userId, circleId, request.getNewRole(), requestingUserId);

        CircleRole newRole = CircleRole.valueOf(request.getNewRole().toUpperCase());
        CircleMember updatedMember = circleService.updateMemberRole(circleId, userId, newRole, requestingUserId);
        CircleMember requestingUserMembership = circleService.getUserMembershipInCircle(circleId, requestingUserId);

        log.info("Role of user {} in circle {} updated to {} successfully", userId, circleId, newRole);
        return ResponseEntity.ok(CircleMapper.toMemberResponse(updatedMember, requestingUserMembership));
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
    public ResponseEntity<CircleListResponse> searchCircles(@RequestParam String searchTerm, @RequestParam Long userId) {
        log.info("Searching circles with term '{}' for user {}", searchTerm, userId);

        List<Circle> allResults = circleService.searchCircles(searchTerm);
        List<Circle> userCircles = circleService.getCirclesForUser(userId);

        // Filter to only circles the user is a member of
        List<Circle> filteredResults = allResults.stream()
                .filter(circle -> userCircles.stream()
                        .anyMatch(userCircle -> userCircle.getId().equals(circle.getId())))
                .toList();

        // Build a map of circle ID to user's membership
        Map<Long, CircleMember> userMemberships = filteredResults.stream()
                .collect(Collectors.toMap(
                        Circle::getId,
                        circle -> circleService.getUserMembershipInCircle(circle.getId(), userId)
                ));

        CircleListResponse response = CircleMapper.toCircleListResponse(filteredResults, userMemberships);

        log.info("Found {} circles matching search term '{}' for user {}", filteredResults.size(), searchTerm, userId);
        return ResponseEntity.ok(response);
    }
}