package com.linkups.api.controller.v1;

import com.linkups.domain.entity.Activity;
import com.linkups.domain.service.ActivityService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Activity Controller - REST API endpoints for activity management
 *
 * This controller provides endpoints for the activity feed functionality,
 * including retrieving activity feeds, marking activities as read, and
 * getting activity statistics.
 */
@RestController
@RequestMapping("/api/activities")
@Slf4j
public class ActivityController {

    private final ActivityService activityService;

    public ActivityController(ActivityService activityService) {
        this.activityService = activityService;
        log.info("ActivityController initialized successfully");
    }

    /**
     * Get activity feed for a user
     *
     * @param userId User ID
     * @param page Page number (default: 0)
     * @param limit Page size (default: 20)
     * @param types Activity types to filter by (comma-separated)
     * @param dateRange Date range filter: "today", "week", "month"
     * @param sort Sort order: "recent" or "priority" (default: "recent")
     * @return Activity feed with pagination metadata
     */
    @GetMapping("/{userId}/feed")
    public ResponseEntity<?> getActivityFeed(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(required = false) String types,
            @RequestParam(required = false) String dateRange,
            @RequestParam(defaultValue = "recent") String sort) {

        log.info("Getting activity feed for user {} with page={}, limit={}, types={}, dateRange={}, sort={}",
                 userId, page, limit, types, dateRange, sort);

        try {
            // Parse activity types
            List<String> activityTypes = null;
            if (types != null && !types.trim().isEmpty()) {
                activityTypes = Arrays.asList(types.split(","));
            }

            // Get activity feed
            Page<Activity> activities = activityService.getActivityFeed(
                userId, page, limit, activityTypes, dateRange, sort
            );

            // Build response
            ActivityFeedResponse response = new ActivityFeedResponse(
                activities.getContent(),
                activities.hasNext(),
                activities.getContent().isEmpty() ? null :
                    activities.getContent().get(activities.getContent().size() - 1).getTimestamp().toString(),
                activities.getTotalElements()
            );

            log.debug("Returning {} activities for user {} (page {}/{})",
                     activities.getContent().size(), userId, page + 1, activities.getTotalPages());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Failed to get activity feed for user {}: {}", userId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                    "error", "Failed to retrieve activity feed",
                    "message", e.getMessage(),
                    "timestamp", java.time.LocalDateTime.now().toString()
                ));
        }
    }

    /**
     * Get activities for specific friends
     *
     * @param userId User ID
     * @param friendIds Friend IDs (comma-separated)
     * @param page Page number (default: 0)
     * @param limit Page size (default: 20)
     * @param types Activity types to filter by (comma-separated)
     * @return Friend activities with pagination
     */
    @GetMapping("/friends/{userId}/activities")
    public ResponseEntity<?> getFriendActivities(
            @PathVariable Long userId,
            @RequestParam String friendIds,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(required = false) String types) {

        log.info("Getting friend activities for user {} with friendIds={}, page={}, limit={}",
                 userId, friendIds, page, limit);

        try {
            // Parse friend IDs
            List<Long> friendIdList = Arrays.stream(friendIds.split(","))
                .map(String::trim)
                .map(Long::parseLong)
                .toList();

            // Parse activity types
            List<String> activityTypes = null;
            if (types != null && !types.trim().isEmpty()) {
                activityTypes = Arrays.asList(types.split(","));
            }

            // Get friend activities
            Page<Activity> activities = activityService.getFriendActivities(
                userId, friendIdList, page, limit, activityTypes
            );

            // Build response
            ActivityFeedResponse response = new ActivityFeedResponse(
                activities.getContent(),
                activities.hasNext(),
                activities.getContent().isEmpty() ? null :
                    activities.getContent().get(activities.getContent().size() - 1).getTimestamp().toString(),
                activities.getTotalElements()
            );

            return ResponseEntity.ok(response);

        } catch (NumberFormatException e) {
            log.warn("Invalid friend IDs format for user {}: {}", userId, friendIds);
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Invalid friend IDs format", "message", "Friend IDs must be comma-separated numbers"));

        } catch (Exception e) {
            log.error("Failed to get friend activities for user {}: {}", userId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                    "error", "Failed to retrieve friend activities",
                    "message", e.getMessage(),
                    "timestamp", java.time.LocalDateTime.now().toString()
                ));
        }
    }

    /**
     * Mark activity as read
     *
     * @param activityId Activity ID
     * @param request Request body containing userId
     * @return Success response
     */
    @PutMapping("/{activityId}/read")
    public ResponseEntity<?> markActivityAsRead(
            @PathVariable Long activityId,
            @RequestBody Map<String, Object> request) {

        log.info("Marking activity {} as read", activityId);

        try {
            // Extract user ID from request
            Long userId = null;
            if (request.containsKey("userId")) {
                Object userIdObj = request.get("userId");
                if (userIdObj instanceof Number) {
                    userId = ((Number) userIdObj).longValue();
                } else if (userIdObj instanceof String) {
                    userId = Long.parseLong((String) userIdObj);
                }
            }

            if (userId == null) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "Missing userId in request body"));
            }

            // Mark activity as read
            Optional<Activity> activity = activityService.markActivityAsRead(activityId, userId);

            if (activity.isPresent()) {
                log.info("Activity {} marked as read by user {}", activityId, userId);
                return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Activity marked as read",
                    "activityId", activityId,
                    "userId", userId
                ));
            } else {
                log.warn("Activity {} not found when marking as read by user {}", activityId, userId);
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Activity not found", "activityId", activityId));
            }

        } catch (NumberFormatException e) {
            log.warn("Invalid userId format when marking activity {} as read", activityId);
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Invalid userId format"));

        } catch (Exception e) {
            log.error("Failed to mark activity {} as read: {}", activityId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                    "error", "Failed to mark activity as read",
                    "message", e.getMessage(),
                    "timestamp", java.time.LocalDateTime.now().toString()
                ));
        }
    }

    /**
     * Get activity statistics for a user
     *
     * @param userId User ID
     * @return Activity statistics
     */
    @GetMapping("/{userId}/stats")
    public ResponseEntity<?> getActivityStatistics(@PathVariable Long userId) {
        log.info("Getting activity statistics for user {}", userId);

        try {
            ActivityService.ActivityStatistics stats = activityService.getActivityStatistics(userId);

            Map<String, Object> response = Map.of(
                "totalActivities", stats.totalActivities,
                "todayActivities", stats.todayActivities,
                "weekActivities", stats.weekActivities,
                "lastUpdated", stats.lastUpdated.toString()
            );

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Failed to get activity statistics for user {}: {}", userId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                    "error", "Failed to retrieve activity statistics",
                    "message", e.getMessage(),
                    "timestamp", java.time.LocalDateTime.now().toString()
                ));
        }
    }

    /**
     * Create a new activity (for testing or integration purposes)
     *
     * @param request Activity creation request
     * @return Created activity
     */
    @PostMapping
    public ResponseEntity<?> createActivity(@RequestBody CreateActivityRequest request) {
        log.info("Creating activity for user {} with type {}", request.userId, request.type);

        try {
            if (request.userId == null || request.type == null) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "Missing required fields: userId and type"));
            }

            Activity activity = activityService.createActivity(
                request.userId,
                request.type,
                request.data,
                request.priority,
                request.visibility,
                request.relatedEntityId,
                request.relatedEntityType
            );

            return ResponseEntity.status(HttpStatus.CREATED).body(activity);

        } catch (Exception e) {
            log.error("Failed to create activity for user {}: {}", request.userId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                    "error", "Failed to create activity",
                    "message", e.getMessage(),
                    "timestamp", java.time.LocalDateTime.now().toString()
                ));
        }
    }

    /**
     * Activity Feed Response DTO
     */
    public static class ActivityFeedResponse {
        public final List<Activity> activities;
        public final boolean hasMore;
        public final String nextCursor;
        public final Long totalCount;

        public ActivityFeedResponse(List<Activity> activities, boolean hasMore, String nextCursor, Long totalCount) {
            this.activities = activities;
            this.hasMore = hasMore;
            this.nextCursor = nextCursor;
            this.totalCount = totalCount;
        }
    }

    /**
     * Create Activity Request DTO
     */
    public static class CreateActivityRequest {
        public Long userId;
        public String type;
        public String data;
        public Integer priority;
        public String visibility;
        public Long relatedEntityId;
        public String relatedEntityType;
    }
}