package com.linkups.domain.service;

import com.linkups.domain.entity.UserStatus;
import com.linkups.domain.entity.enums.UserStatusType;
import com.linkups.domain.exception.InvalidOperationException;
import com.linkups.domain.exception.ResourceNotFoundException;
import com.linkups.domain.repository.UserStatusRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class UserStatusService {

    private final UserStatusRepository userStatusRepository;

    // Business rules configuration
    private static final int MIN_STATUS_CHANGE_INTERVAL_MINUTES = 1; // Prevent spam status changes
    private static final int INACTIVE_USER_THRESHOLD_DAYS = 30; // For cleanup operations

    /**
     * Get user status by user ID
     */
    public Optional<UserStatus> getUserStatus(Long userId) {
        log.debug("Getting status for user: {}", userId);
        return userStatusRepository.findByUserId(userId);
    }

    /**
     * Update user status with business rule validation
     */
    public UserStatus updateUserStatus(Long userId, UserStatusType newStatus) {
        log.info("Updating status for user {} to {}", userId, newStatus);

        LocalDateTime now = LocalDateTime.now();
        Optional<UserStatus> existingStatusOpt = userStatusRepository.findByUserId(userId);

        if (existingStatusOpt.isPresent()) {
            UserStatus existingStatus = existingStatusOpt.get();

            // Business rule validations
            validateStatusChange(existingStatus, newStatus, userId);

            // Update existing status
            existingStatus.setStatus(newStatus);
            existingStatus.setLastSeen(now);
            existingStatus.setUpdatedAt(now);

            UserStatus savedStatus = userStatusRepository.save(existingStatus);
            log.info("Updated user {} status from {} to {}", userId, existingStatus.getStatus(), newStatus);
            return savedStatus;

        } else {
            // Create new status record
            UserStatus newUserStatus = UserStatus.builder()
                    .userId(userId)
                    .status(newStatus)
                    .lastSeen(now)
                    .createdAt(now)
                    .updatedAt(now)
                    .build();

            UserStatus savedStatus = userStatusRepository.save(newUserStatus);
            log.info("Created new user status for user {} with status {}", userId, newStatus);
            return savedStatus;
        }
    }

    /**
     * Get statuses for multiple users efficiently (for friend lists)
     */
    @Transactional(readOnly = true)
    public Map<Long, UserStatus> getUserStatuses(List<Long> userIds) {
        log.debug("Getting statuses for {} users", userIds.size());

        if (userIds.isEmpty()) {
            return Map.of();
        }

        List<UserStatus> statuses = userStatusRepository.findByUserIdIn(userIds);

        Map<Long, UserStatus> statusMap = statuses.stream()
                .collect(Collectors.toMap(
                    UserStatus::getUserId,
                    status -> status
                ));

        log.debug("Retrieved {} statuses for {} requested users", statusMap.size(), userIds.size());
        return statusMap;
    }

    /**
     * Get all online users (excluding offline)
     */
    @Transactional(readOnly = true)
    public List<UserStatus> getOnlineUsers() {
        log.debug("Getting all online users");
        return userStatusRepository.findAllOnlineUsers(UserStatusType.OFFLINE);
    }

    /**
     * Get users with specific status types
     */
    @Transactional(readOnly = true)
    public List<UserStatus> getUsersByStatuses(List<UserStatusType> statuses) {
        log.debug("Getting users with statuses: {}", statuses);
        return userStatusRepository.findByStatusIn(statuses);
    }

    /**
     * Set user offline (typically called on logout)
     */
    public UserStatus setUserOffline(Long userId) {
        log.info("Setting user {} offline", userId);
        return updateUserStatus(userId, UserStatusType.OFFLINE);
    }

    /**
     * Set user online (typically called on login)
     */
    public UserStatus setUserOnline(Long userId) {
        log.info("Setting user {} online", userId);
        return updateUserStatus(userId, UserStatusType.ONLINE);
    }

    /**
     * Update last seen timestamp without changing status (heartbeat)
     */
    public void updateLastSeen(Long userId) {
        log.debug("Updating last seen for user: {}", userId);

        LocalDateTime now = LocalDateTime.now();
        Optional<UserStatus> statusOpt = userStatusRepository.findByUserId(userId);

        if (statusOpt.isPresent()) {
            UserStatus userStatus = statusOpt.get();
            userStatus.setLastSeen(now);
            userStatus.setUpdatedAt(now);
            userStatusRepository.save(userStatus);
            log.debug("Updated last seen for user {}", userId);
        } else {
            // Create with ONLINE status if no status exists
            log.info("No status found for user {}, creating with ONLINE status", userId);
            updateUserStatus(userId, UserStatusType.ONLINE);
        }
    }

    /**
     * Get status statistics (for admin dashboard)
     */
    @Transactional(readOnly = true)
    public Map<UserStatusType, Long> getStatusStatistics() {
        log.debug("Getting status statistics");

        return Map.of(
            UserStatusType.ONLINE, userStatusRepository.countByStatus(UserStatusType.ONLINE),
            UserStatusType.OFFLINE, userStatusRepository.countByStatus(UserStatusType.OFFLINE),
            UserStatusType.BUSY, userStatusRepository.countByStatus(UserStatusType.BUSY),
            UserStatusType.AWAY, userStatusRepository.countByStatus(UserStatusType.AWAY),
            UserStatusType.DO_NOT_DISTURB, userStatusRepository.countByStatus(UserStatusType.DO_NOT_DISTURB)
        );
    }

    /**
     * Clean up inactive users (admin operation)
     */
    public List<UserStatus> findInactiveUsers() {
        LocalDateTime cutoffTime = LocalDateTime.now().minusDays(INACTIVE_USER_THRESHOLD_DAYS);
        log.info("Finding users inactive since: {}", cutoffTime);

        return userStatusRepository.findUsersNotSeenSince(cutoffTime);
    }

    /**
     * Validate status change according to business rules
     */
    private void validateStatusChange(UserStatus currentStatus, UserStatusType newStatus, Long userId) {
        UserStatusType currentStatusType = currentStatus.getStatus();

        // Rule 1: Cannot set the same status
        if (currentStatusType == newStatus) {
            throw InvalidOperationException.cannotSetSameStatus(currentStatusType);
        }

        // Rule 2: Rate limiting - prevent rapid status changes
        LocalDateTime lastUpdate = currentStatus.getUpdatedAt();
        long minutesSinceLastUpdate = ChronoUnit.MINUTES.between(lastUpdate, LocalDateTime.now());

        if (minutesSinceLastUpdate < MIN_STATUS_CHANGE_INTERVAL_MINUTES) {
            throw InvalidOperationException.statusNotChangedRecentlyEnough(userId, MIN_STATUS_CHANGE_INTERVAL_MINUTES);
        }

        // Rule 3: Custom status transition rules (if needed)
        if (!isValidStatusTransition(currentStatusType, newStatus)) {
            throw InvalidOperationException.invalidStatusTransition(currentStatusType, newStatus);
        }

        log.debug("Status change validation passed for user {}: {} -> {}", userId, currentStatusType, newStatus);
    }

    /**
     * Define valid status transitions (can be expanded with business rules)
     */
    private boolean isValidStatusTransition(UserStatusType from, UserStatusType to) {
        // For now, allow all transitions except to the same status
        // This can be expanded with specific business rules if needed
        // Example: Maybe OFFLINE -> BUSY is not allowed directly

        return from != to;
    }
}
