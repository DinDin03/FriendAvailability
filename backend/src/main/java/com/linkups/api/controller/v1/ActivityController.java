package com.linkups.api.controller.v1;

import com.linkups.domain.entity.Activity;
import com.linkups.domain.exception.ValidationException;
import com.linkups.domain.service.ActivityService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

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
    public ResponseEntity<ActivityFeedResponse> getActivityFeed(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(required = false) String types,
            @RequestParam(required = false) String dateRange,
            @RequestParam(defaultValue = "recent") String sort) {

        log.info("Getting activity feed for user {} with page={}, limit={}, types={}, dateRange={}, sort={}",
                 userId, page, limit, types, dateRange, sort);

        List<String> activityTypes = parseActivityTypes(types);
        Page<Activity> activities = activityService.getActivityFeed(userId, page, limit, activityTypes, dateRange, sort);
        ActivityFeedResponse response = buildActivityFeedResponse(activities);

        log.debug("Returning {} activities for user {} (page {}/{})",
                 activities.getContent().size(), userId, page + 1, activities.getTotalPages());

        return ResponseEntity.ok(response);
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
    public ResponseEntity<ActivityFeedResponse> getFriendActivities(
            @PathVariable Long userId,
            @RequestParam String friendIds,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(required = false) String types) {

        log.info("Getting friend activities for user {} with friendIds={}, page={}, limit={}",
                 userId, friendIds, page, limit);

        List<Long> friendIdList = parseFriendIds(friendIds);
        List<String> activityTypes = parseActivityTypes(types);
        Page<Activity> activities = activityService.getFriendActivities(userId, friendIdList, page, limit, activityTypes);
        ActivityFeedResponse response = buildActivityFeedResponse(activities);

        return ResponseEntity.ok(response);
    }

    /**
     * Mark activity as read
     *
     * @param activityId Activity ID
     * @param request Request body containing userId
     * @return Success response
     */
    @PutMapping("/{activityId}/read")
    public ResponseEntity<Map<String, Object>> markActivityAsRead(
            @PathVariable Long activityId,
            @RequestBody Map<String, Object> request) {

        log.info("Marking activity {} as read", activityId);

        Long userId = extractUserId(request);
        Activity activity = activityService.markActivityAsRead(activityId, userId);

        log.info("Activity {} marked as read by user {}", activityId, userId);
        return ResponseEntity.ok(Map.of(
            "success", true,
            "message", "Activity marked as read",
            "activityId", activityId,
            "userId", userId
        ));
    }

    /**
     * Get activity statistics for a user
     *
     * @param userId User ID
     * @return Activity statistics
     */
    @GetMapping("/{userId}/stats")
    public ResponseEntity<Map<String, Object>> getActivityStatistics(@PathVariable Long userId) {
        log.info("Getting activity statistics for user {}", userId);

        ActivityService.ActivityStatistics stats = activityService.getActivityStatistics(userId);

        Map<String, Object> response = Map.of(
            "totalActivities", stats.totalActivities,
            "todayActivities", stats.todayActivities,
            "weekActivities", stats.weekActivities,
            "lastUpdated", stats.lastUpdated.toString()
        );

        return ResponseEntity.ok(response);
    }

    /**
     * Create a new activity (for testing or integration purposes)
     *
     * @param request Activity creation request
     * @return Created activity
     */
    @PostMapping
    public ResponseEntity<Activity> createActivity(@RequestBody CreateActivityRequest request) {
        log.info("Creating activity for user {} with type {}", request.userId, request.type);

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
    }

    // Helper methods

    private List<String> parseActivityTypes(String types) {
        if (types == null || types.trim().isEmpty()) {
            return null;
        }
        return Arrays.asList(types.split(","));
    }

    private List<Long> parseFriendIds(String friendIds) {
        try {
            return Arrays.stream(friendIds.split(","))
                .map(String::trim)
                .map(Long::parseLong)
                .toList();
        } catch (NumberFormatException e) {
            throw ValidationException.invalidFieldValue("friendIds", "Friend IDs must be comma-separated numbers");
        }
    }

    private Long extractUserId(Map<String, Object> request) {
        if (!request.containsKey("userId")) {
            throw ValidationException.invalidFieldValue("userId", "Missing userId in request body");
        }

        Object userIdObj = request.get("userId");
        try {
            if (userIdObj instanceof Number) {
                return ((Number) userIdObj).longValue();
            } else if (userIdObj instanceof String) {
                return Long.parseLong((String) userIdObj);
            }
            throw ValidationException.invalidFieldValue("userId", "Invalid userId format");
        } catch (NumberFormatException e) {
            throw ValidationException.invalidFieldValue("userId", "Invalid userId format");
        }
    }

    private ActivityFeedResponse buildActivityFeedResponse(Page<Activity> activities) {
        return new ActivityFeedResponse(
            activities.getContent(),
            activities.hasNext(),
            activities.getContent().isEmpty() ? null :
                activities.getContent().get(activities.getContent().size() - 1).getTimestamp().toString(),
            activities.getTotalElements()
        );
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