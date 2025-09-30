package com.linkups.api.controller.v1;

import com.linkups.api.dto.request.userstatus.BatchStatusRequestDTO;
import com.linkups.api.dto.request.userstatus.UpdateUserStatusRequestDTO;
import com.linkups.api.dto.response.common.SuccessResponseDTO;
import com.linkups.api.dto.response.userstatus.BatchUserStatusResponseDTO;
import com.linkups.api.dto.response.userstatus.StatusStatisticsDTO;
import com.linkups.api.dto.response.userstatus.UserStatusResponseDTO;
import com.linkups.api.mapper.UserStatusMapper;
import com.linkups.domain.entity.UserStatus;
import com.linkups.domain.service.UserStatusService;
import jakarta.validation.Valid;
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
    public ResponseEntity<UserStatusResponseDTO> getUserStatus(@PathVariable Long userId) {
        log.info("GET /api/users/{}/status", userId);

        UserStatus userStatus = userStatusService.getUserStatus(userId)
                .orElseThrow(() -> new com.linkups.domain.exception.ResourceNotFoundException("User status not found for user ID: " + userId));
        UserStatusResponseDTO response = UserStatusMapper.toUserStatusResponse(userStatus);

        return ResponseEntity.ok(response);
    }

    /**
     * Update user status
     * PUT /api/users/{userId}/status
     */
    @PutMapping("/{userId}/status")
    public ResponseEntity<UserStatusResponseDTO> updateUserStatus(
            @PathVariable Long userId,
            @Valid @RequestBody UpdateUserStatusRequestDTO request) {
        log.info("PUT /api/users/{}/status", userId);

        Map<String, String> statusMap = Map.of(
                "status", request.getStatus(),
                "currentActivity", request.getCurrentActivity() != null ? request.getCurrentActivity() : ""
        );

        UserStatus userStatus = userStatusService.parseAndUpdateUserStatus(userId, statusMap);
        UserStatusResponseDTO response = UserStatusMapper.toUserStatusResponse(userStatus);

        return ResponseEntity.ok(response);
    }

    /**
     * Update last seen (heartbeat)
     * POST /api/users/{userId}/heartbeat
     */
    @PostMapping("/{userId}/heartbeat")
    public ResponseEntity<SuccessResponseDTO> updateLastSeen(@PathVariable Long userId) {
        log.debug("POST /api/users/{}/heartbeat", userId);

        userStatusService.updateLastSeen(userId);
        SuccessResponseDTO response = SuccessResponseDTO.of("Last seen updated");

        return ResponseEntity.ok(response);
    }

    /**
     * Get statuses for multiple users (for friend lists)
     * POST /api/users/statuses (using POST to send user ID list in body)
     */
    @PostMapping("/statuses")
    public ResponseEntity<BatchUserStatusResponseDTO> getUserStatuses(@Valid @RequestBody Map<String, List<Long>> request) {
        log.info("POST /api/users/statuses");

        List<Long> userIds = request.get("userIds");
        userStatusService.validateBatchUserIds(userIds);
        Map<Long, UserStatus> statusMap = userStatusService.getUserStatuses(userIds);
        List<UserStatus> statuses = new java.util.ArrayList<>(statusMap.values());
        BatchUserStatusResponseDTO response = UserStatusMapper.toBatchUserStatusResponse(statuses);

        return ResponseEntity.ok(response);
    }

    /**
     * Get all online users
     * GET /api/users/online
     */
    @GetMapping("/online")
    public ResponseEntity<BatchUserStatusResponseDTO> getOnlineUsers() {
        log.info("GET /api/users/online");

        List<UserStatus> onlineStatuses = userStatusService.getOnlineUsers();
        BatchUserStatusResponseDTO response = UserStatusMapper.toBatchUserStatusResponse(onlineStatuses);

        return ResponseEntity.ok(response);
    }

    /**
     * Get status statistics (admin/analytics endpoint)
     * GET /api/users/status-statistics
     */
    @GetMapping("/status-statistics")
    public ResponseEntity<StatusStatisticsDTO> getStatusStatistics() {
        log.info("GET /api/users/status-statistics");

        Map<com.linkups.domain.entity.enums.UserStatusType, Long> rawStats = userStatusService.getStatusStatistics();
        Map<String, Long> stats = new java.util.HashMap<>();
        rawStats.forEach((key, value) -> stats.put(key.toString(), value));
        StatusStatisticsDTO response = UserStatusMapper.toStatusStatistics(stats);

        return ResponseEntity.ok(response);
    }

    /**
     * Set user online (typically called on login)
     * POST /api/users/{userId}/online
     */
    @PostMapping("/{userId}/online")
    public ResponseEntity<UserStatusResponseDTO> setUserOnline(@PathVariable Long userId) {
        log.info("POST /api/users/{}/online", userId);

        UserStatus userStatus = userStatusService.setUserOnline(userId);
        UserStatusResponseDTO response = UserStatusMapper.toUserStatusResponse(userStatus);

        return ResponseEntity.ok(response);
    }

    /**
     * Set user offline (typically called on logout)
     * POST /api/users/{userId}/offline
     */
    @PostMapping("/{userId}/offline")
    public ResponseEntity<UserStatusResponseDTO> setUserOffline(@PathVariable Long userId) {
        log.info("POST /api/users/{}/offline", userId);

        UserStatus userStatus = userStatusService.setUserOffline(userId);
        UserStatusResponseDTO response = UserStatusMapper.toUserStatusResponse(userStatus);

        return ResponseEntity.ok(response);
    }
}