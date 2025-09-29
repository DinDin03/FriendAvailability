package com.linkups.api.controller.v1;

import com.linkups.domain.entity.UserStatus;
import com.linkups.domain.entity.enums.UserStatusType;
import com.linkups.domain.service.UserStatusService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = {"http://localhost:8080", "http://localhost:5173", "http://localhost:5174","http://127.0.0.1:8080", "https://friendavailability-production.up.railway.app", "https://www.linkups.com.au"})
@RequiredArgsConstructor
@Slf4j
public class UserStatusController {

    private final UserStatusService userStatusService;

    /**
     * Get user status by user ID
     * GET /api/users/{userId}/status
     */
    @GetMapping("/{userId}/status")
    public ResponseEntity<?> getUserStatus(@PathVariable Long userId) {
        log.info("GET /api/users/{}/status - Getting status for user", userId);

        try {
            Optional<UserStatus> userStatus = userStatusService.getUserStatus(userId);

            if (userStatus.isPresent()) {
                log.debug("Found status for user {}: {}", userId, userStatus.get().getStatus());
                return ResponseEntity.ok(userStatus.get());
            } else {
                log.info("No status found for user {}, returning default offline status", userId);
                // Return default status instead of 404
                return ResponseEntity.ok(Map.of(
                    "userId", userId,
                    "status", UserStatusType.OFFLINE,
                    "message", "No status record found, defaulting to OFFLINE"
                ));
            }
        } catch (Exception e) {
            log.error("Error getting status for user {}: {}", userId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to retrieve user status"));
        }
    }

    /**
     * Update user status
     * PUT /api/users/{userId}/status
     */
    @PutMapping("/{userId}/status")
    public ResponseEntity<?> updateUserStatus(
            @PathVariable Long userId,
            @RequestBody Map<String, String> request) {

        log.info("PUT /api/users/{}/status - Updating status", userId);

        try {
            String statusString = request.get("status");
            if (statusString == null || statusString.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Status is required"));
            }

            UserStatusType newStatus;
            try {
                newStatus = UserStatusType.valueOf(statusString.toUpperCase());
            } catch (IllegalArgumentException e) {
                log.warn("Invalid status provided: {}", statusString);
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Invalid status. Valid statuses: " +
                                List.of(UserStatusType.values())));
            }

            UserStatus updatedStatus = userStatusService.updateUserStatus(userId, newStatus);
            log.info("Successfully updated status for user {} to {}", userId, newStatus);

            return ResponseEntity.ok(updatedStatus);

        } catch (Exception e) {
            log.error("Error updating status for user {}: {}", userId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to update user status"));
        }
    }

    /**
     * Update last seen (heartbeat)
     * POST /api/users/{userId}/heartbeat
     */
    @PostMapping("/{userId}/heartbeat")
    public ResponseEntity<?> updateLastSeen(@PathVariable Long userId) {
        log.debug("POST /api/users/{}/heartbeat - Updating last seen", userId);

        try {
            userStatusService.updateLastSeen(userId);
            log.debug("Updated last seen for user {}", userId);

            return ResponseEntity.ok(Map.of(
                "message", "Last seen updated successfully",
                "userId", userId,
                "timestamp", java.time.LocalDateTime.now()
            ));

        } catch (Exception e) {
            log.error("Error updating last seen for user {}: {}", userId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to update last seen"));
        }
    }

    /**
     * Get statuses for multiple users (for friend lists)
     * POST /api/users/statuses (using POST to send user ID list in body)
     */
    @PostMapping("/statuses")
    public ResponseEntity<?> getUserStatuses(@RequestBody Map<String, List<Long>> request) {
        log.info("POST /api/users/statuses - Getting batch statuses");

        try {
            List<Long> userIds = request.get("userIds");
            if (userIds == null || userIds.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "User IDs list is required"));
            }

            if (userIds.size() > 100) { // Reasonable limit
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Too many user IDs requested (max 100)"));
            }

            Map<Long, UserStatus> statuses = userStatusService.getUserStatuses(userIds);
            log.info("Retrieved {} statuses for {} requested users", statuses.size(), userIds.size());

            return ResponseEntity.ok(Map.of(
                "statuses", statuses,
                "requestedCount", userIds.size(),
                "foundCount", statuses.size()
            ));

        } catch (Exception e) {
            log.error("Error getting batch statuses: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to retrieve user statuses"));
        }
    }

    /**
     * Get all online users
     * GET /api/users/online
     */
    @GetMapping("/online")
    public ResponseEntity<?> getOnlineUsers() {
        log.info("GET /api/users/online - Getting all online users");

        try {
            List<UserStatus> onlineUsers = userStatusService.getOnlineUsers();
            log.info("Found {} online users", onlineUsers.size());

            return ResponseEntity.ok(Map.of(
                "onlineUsers", onlineUsers,
                "count", onlineUsers.size()
            ));

        } catch (Exception e) {
            log.error("Error getting online users: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to retrieve online users"));
        }
    }

    /**
     * Get status statistics (admin/analytics endpoint)
     * GET /api/users/status-statistics
     */
    @GetMapping("/status-statistics")
    public ResponseEntity<?> getStatusStatistics() {
        log.info("GET /api/users/status-statistics - Getting status statistics");

        try {
            Map<UserStatusType, Long> statistics = userStatusService.getStatusStatistics();
            log.info("Retrieved status statistics: {}", statistics);

            return ResponseEntity.ok(Map.of(
                "statistics", statistics,
                "totalUsers", statistics.values().stream().mapToLong(Long::longValue).sum(),
                "timestamp", java.time.LocalDateTime.now()
            ));

        } catch (Exception e) {
            log.error("Error getting status statistics: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to retrieve status statistics"));
        }
    }

    /**
     * Set user online (typically called on login)
     * POST /api/users/{userId}/online
     */
    @PostMapping("/{userId}/online")
    public ResponseEntity<?> setUserOnline(@PathVariable Long userId) {
        log.info("POST /api/users/{}/online - Setting user online", userId);

        try {
            UserStatus updatedStatus = userStatusService.setUserOnline(userId);
            log.info("Set user {} online successfully", userId);

            return ResponseEntity.ok(updatedStatus);

        } catch (Exception e) {
            log.error("Error setting user {} online: {}", userId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to set user online"));
        }
    }

    /**
     * Set user offline (typically called on logout)
     * POST /api/users/{userId}/offline
     */
    @PostMapping("/{userId}/offline")
    public ResponseEntity<?> setUserOffline(@PathVariable Long userId) {
        log.info("POST /api/users/{}/offline - Setting user offline", userId);

        try {
            UserStatus updatedStatus = userStatusService.setUserOffline(userId);
            log.info("Set user {} offline successfully", userId);

            return ResponseEntity.ok(updatedStatus);

        } catch (Exception e) {
            log.error("Error setting user {} offline: {}", userId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to set user offline"));
        }
    }
}