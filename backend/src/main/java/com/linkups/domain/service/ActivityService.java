package com.linkups.domain.service;

import com.linkups.domain.entity.Activity;
import com.linkups.domain.repository.ActivityRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Activity Service - Business logic for managing user activities
 *
 * This service handles the creation, retrieval, and management of user activities
 * for the social activity feed functionality.
 */
@Service
@Slf4j
@Transactional
public class ActivityService {

    private final ActivityRepository activityRepository;
    private final FriendService friendService;

    // Default visibility levels for activity feeds
    private static final List<String> DEFAULT_VISIBILITY = Arrays.asList("public", "friends");

    public ActivityService(ActivityRepository activityRepository, FriendService friendService) {
        this.activityRepository = activityRepository;
        this.friendService = friendService;
        log.info("ActivityService initialized successfully");
    }

    /**
     * Get activity feed for a user
     *
     * @param userId User ID requesting the feed
     * @param page Page number (0-based)
     * @param size Page size
     * @param activityTypes Optional list of activity types to filter by
     * @param dateRange Date range filter: "today", "week", "month", or null for all
     * @param sort Sort order: "recent" or "priority"
     * @return Page of activities
     */
    @Transactional(readOnly = true)
    public Page<Activity> getActivityFeed(Long userId, int page, int size, List<String> activityTypes, String dateRange, String sort) {
        log.debug("Getting activity feed for user {} with page={}, size={}, types={}, dateRange={}, sort={}",
                  userId, page, size, activityTypes, dateRange, sort);

        try {
            // Get user's friends
            List<Long> friendIds = friendService.getFriendIds(userId);

            if (friendIds.isEmpty()) {
                log.debug("User {} has no friends, returning empty feed", userId);
                return Page.empty();
            }

            // Create pagination
            Sort sortOrder = createSortOrder(sort);
            Pageable pageable = PageRequest.of(page, size, sortOrder);

            // Determine date range
            LocalDateTime startDate = null;
            LocalDateTime endDate = LocalDateTime.now();

            if (dateRange != null) {
                startDate = calculateStartDate(dateRange, endDate);
            }

            // Query activities based on filters
            Page<Activity> activities;

            if (activityTypes != null && !activityTypes.isEmpty() && startDate != null) {
                // Both type and date filtering
                activities = activityRepository.findActivitiesByFriendsTypesAndDateRange(
                    friendIds, activityTypes, startDate, endDate, true, DEFAULT_VISIBILITY, pageable
                );
            } else if (activityTypes != null && !activityTypes.isEmpty()) {
                // Type filtering only
                activities = activityRepository.findActivitiesByFriendsAndTypes(
                    friendIds, activityTypes, true, DEFAULT_VISIBILITY, pageable
                );
            } else if (startDate != null) {
                // Date filtering only
                activities = activityRepository.findActivitiesByFriendsAndDateRange(
                    friendIds, startDate, endDate, true, DEFAULT_VISIBILITY, pageable
                );
            } else {
                // No filtering
                activities = activityRepository.findActivitiesByFriends(
                    friendIds, true, DEFAULT_VISIBILITY, pageable
                );
            }

            log.debug("Retrieved {} activities for user {}", activities.getContent().size(), userId);
            return activities;

        } catch (Exception e) {
            log.error("Failed to get activity feed for user {}: {}", userId, e.getMessage(), e);
            throw new RuntimeException("Failed to retrieve activity feed", e);
        }
    }

    /**
     * Get activities for specific friends
     *
     * @param userId User ID requesting the activities
     * @param friendIds List of friend IDs to get activities for
     * @param page Page number
     * @param size Page size
     * @param activityTypes Optional activity type filters
     * @return Page of activities
     */
    @Transactional(readOnly = true)
    public Page<Activity> getFriendActivities(Long userId, List<Long> friendIds, int page, int size, List<String> activityTypes) {
        log.debug("Getting activities for friends {} requested by user {}", friendIds, userId);

        try {
            // Verify friendship relationships
            List<Long> verifiedFriendIds = friendService.getVerifiedFriendIds(userId, friendIds);

            if (verifiedFriendIds.isEmpty()) {
                return Page.empty();
            }

            Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "timestamp"));

            Page<Activity> activities;
            if (activityTypes != null && !activityTypes.isEmpty()) {
                activities = activityRepository.findActivitiesByFriendsAndTypes(
                    verifiedFriendIds, activityTypes, true, DEFAULT_VISIBILITY, pageable
                );
            } else {
                activities = activityRepository.findActivitiesByFriends(
                    verifiedFriendIds, true, DEFAULT_VISIBILITY, pageable
                );
            }

            log.debug("Retrieved {} activities for friends of user {}", activities.getContent().size(), userId);
            return activities;

        } catch (Exception e) {
            log.error("Failed to get friend activities for user {}: {}", userId, e.getMessage(), e);
            throw new RuntimeException("Failed to retrieve friend activities", e);
        }
    }

    /**
     * Create a new activity
     *
     * @param userId User who performed the activity
     * @param type Activity type
     * @param data Activity-specific data (JSON string)
     * @param priority Priority level (1-5)
     * @param visibility Visibility level
     * @param relatedEntityId Optional related entity ID
     * @param relatedEntityType Optional related entity type
     * @return Created activity
     */
    public Activity createActivity(Long userId, String type, String data, Integer priority,
                                 String visibility, Long relatedEntityId, String relatedEntityType) {
        log.debug("Creating activity for user {} with type {}", userId, type);

        try {
            Activity activity = Activity.builder()
                .userId(userId)
                .type(type)
                .data(data)
                .priority(priority != null ? priority : 1)
                .visibility(visibility != null ? visibility : "friends")
                .relatedEntityId(relatedEntityId)
                .relatedEntityType(relatedEntityType)
                .timestamp(LocalDateTime.now())
                .isActive(true)
                .build();

            Activity savedActivity = activityRepository.save(activity);
            log.info("Created activity {} for user {} with type {}", savedActivity.getId(), userId, type);

            return savedActivity;

        } catch (Exception e) {
            log.error("Failed to create activity for user {}: {}", userId, e.getMessage(), e);
            throw new RuntimeException("Failed to create activity", e);
        }
    }

    /**
     * Mark activity as read by updating its data
     * Note: This is a simplified implementation. In a full system, you'd have a separate ActivityRead entity.
     *
     * @param activityId Activity ID
     * @param userId User who read the activity
     * @return Updated activity or empty if not found
     */
    public Optional<Activity> markActivityAsRead(Long activityId, Long userId) {
        log.debug("Marking activity {} as read by user {}", activityId, userId);

        try {
            Optional<Activity> activityOpt = activityRepository.findById(activityId);

            if (activityOpt.isPresent()) {
                Activity activity = activityOpt.get();

                // For now, we'll just log the read action
                // In a full implementation, you'd track reads in a separate table
                log.info("Activity {} marked as read by user {}", activityId, userId);

                return activityOpt;
            } else {
                log.warn("Activity {} not found when trying to mark as read by user {}", activityId, userId);
                return Optional.empty();
            }

        } catch (Exception e) {
            log.error("Failed to mark activity {} as read by user {}: {}", activityId, userId, e.getMessage(), e);
            throw new RuntimeException("Failed to mark activity as read", e);
        }
    }

    /**
     * Get activity statistics for a user
     *
     * @param userId User ID
     * @return Activity statistics
     */
    @Transactional(readOnly = true)
    public ActivityStatistics getActivityStatistics(Long userId) {
        log.debug("Getting activity statistics for user {}", userId);

        try {
            List<Long> friendIds = friendService.getFriendIds(userId);

            if (friendIds.isEmpty()) {
                return new ActivityStatistics(0L, 0L, 0L, LocalDateTime.now());
            }

            LocalDateTime now = LocalDateTime.now();
            LocalDateTime todayStart = now.truncatedTo(ChronoUnit.DAYS);
            LocalDateTime weekStart = now.minus(7, ChronoUnit.DAYS);

            // Count total activities from friends
            Long totalActivities = (long) activityRepository.findActivitiesByFriends(
                friendIds, true, DEFAULT_VISIBILITY, Pageable.unpaged()
            ).getContent().size();

            // Count today's activities
            Long todayActivities = activityRepository.countActivitiesByUserAndDateRange(
                userId, todayStart, now, true
            );

            // Count this week's activities
            Long weekActivities = activityRepository.countActivitiesByUserAndDateRange(
                userId, weekStart, now, true
            );

            ActivityStatistics stats = new ActivityStatistics(totalActivities, todayActivities, weekActivities, now);
            log.debug("Retrieved statistics for user {}: {}", userId, stats);

            return stats;

        } catch (Exception e) {
            log.error("Failed to get activity statistics for user {}: {}", userId, e.getMessage(), e);
            throw new RuntimeException("Failed to retrieve activity statistics", e);
        }
    }

    /**
     * Deactivate old activities (cleanup method)
     *
     * @param cutoffDate Activities older than this will be deactivated
     * @return Number of deactivated activities
     */
    public Long deactivateOldActivities(LocalDateTime cutoffDate) {
        log.info("Deactivating activities older than {}", cutoffDate);

        try {
            // This would require a custom query to update isActive flag
            // For now, just return 0 as placeholder
            return 0L;

        } catch (Exception e) {
            log.error("Failed to deactivate old activities: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to deactivate old activities", e);
        }
    }

    /**
     * Create sort order based on sort parameter
     */
    private Sort createSortOrder(String sort) {
        if ("priority".equals(sort)) {
            return Sort.by(Sort.Direction.DESC, "priority", "timestamp");
        } else {
            // Default to recent (timestamp desc)
            return Sort.by(Sort.Direction.DESC, "timestamp");
        }
    }

    /**
     * Calculate start date based on date range
     */
    private LocalDateTime calculateStartDate(String dateRange, LocalDateTime endDate) {
        switch (dateRange.toLowerCase()) {
            case "today":
                return endDate.truncatedTo(ChronoUnit.DAYS);
            case "week":
                return endDate.minus(7, ChronoUnit.DAYS);
            case "month":
                return endDate.minus(30, ChronoUnit.DAYS);
            default:
                return null;
        }
    }

    /**
     * Activity Statistics DTO
     */
    public static class ActivityStatistics {
        public final Long totalActivities;
        public final Long todayActivities;
        public final Long weekActivities;
        public final LocalDateTime lastUpdated;

        public ActivityStatistics(Long totalActivities, Long todayActivities, Long weekActivities, LocalDateTime lastUpdated) {
            this.totalActivities = totalActivities;
            this.todayActivities = todayActivities;
            this.weekActivities = weekActivities;
            this.lastUpdated = lastUpdated;
        }

        @Override
        public String toString() {
            return String.format("ActivityStatistics{totalActivities=%d, todayActivities=%d, weekActivities=%d, lastUpdated=%s}",
                               totalActivities, todayActivities, weekActivities, lastUpdated);
        }
    }
}