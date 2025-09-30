package com.linkups.api.controller.v1;

import com.linkups.domain.entity.UserStatus;
import com.linkups.domain.service.UserStatusService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

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
        log.info("GET /api/users/{}/status", userId);
        return ResponseEntity.ok(userStatusService.getUserStatusResponse(userId));
    }

    /**
     * Update user status
     * PUT /api/users/{userId}/status
     */
    @PutMapping("/{userId}/status")
    public ResponseEntity<UserStatus> updateUserStatus(
            @PathVariable Long userId,
            @RequestBody Map<String, String> request) {
        log.info("PUT /api/users/{}/status", userId);
        return ResponseEntity.ok(userStatusService.parseAndUpdateUserStatus(userId, request));
    }

    /**
     * Update last seen (heartbeat)
     * POST /api/users/{userId}/heartbeat
     */
    @PostMapping("/{userId}/heartbeat")
    public ResponseEntity<?> updateLastSeen(@PathVariable Long userId) {
        log.debug("POST /api/users/{}/heartbeat", userId);
        return ResponseEntity.ok(userStatusService.updateLastSeenResponse(userId));
    }

    /**
     * Get statuses for multiple users (for friend lists)
     * POST /api/users/statuses (using POST to send user ID list in body)
     */
    @PostMapping("/statuses")
    public ResponseEntity<?> getUserStatuses(@RequestBody Map<String, List<Long>> request) {
        log.info("POST /api/users/statuses");
        return ResponseEntity.ok(userStatusService.getBatchStatusesResponse(request));
    }

    /**
     * Get all online users
     * GET /api/users/online
     */
    @GetMapping("/online")
    public ResponseEntity<?> getOnlineUsers() {
        log.info("GET /api/users/online");
        return ResponseEntity.ok(userStatusService.getOnlineUsersResponse());
    }

    /**
     * Get status statistics (admin/analytics endpoint)
     * GET /api/users/status-statistics
     */
    @GetMapping("/status-statistics")
    public ResponseEntity<?> getStatusStatistics() {
        log.info("GET /api/users/status-statistics");
        return ResponseEntity.ok(userStatusService.getFormattedStatusStatistics());
    }

    /**
     * Set user online (typically called on login)
     * POST /api/users/{userId}/online
     */
    @PostMapping("/{userId}/online")
    public ResponseEntity<UserStatus> setUserOnline(@PathVariable Long userId) {
        log.info("POST /api/users/{}/online", userId);
        return ResponseEntity.ok(userStatusService.setUserOnline(userId));
    }

    /**
     * Set user offline (typically called on logout)
     * POST /api/users/{userId}/offline
     */
    @PostMapping("/{userId}/offline")
    public ResponseEntity<UserStatus> setUserOffline(@PathVariable Long userId) {
        log.info("POST /api/users/{}/offline", userId);
        return ResponseEntity.ok(userStatusService.setUserOffline(userId));
    }
}