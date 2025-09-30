package com.linkups.api.controller.v1;

import com.linkups.api.dto.request.activity.CreateActivityRequestDTO;
import com.linkups.api.dto.request.activity.MarkActivityReadRequestDTO;
import com.linkups.api.dto.response.activity.ActivityFeedResponseDTO;
import com.linkups.api.dto.response.activity.ActivityResponseDTO;
import com.linkups.api.dto.response.activity.ActivityStatisticsDTO;
import com.linkups.api.dto.response.common.SuccessResponseDTO;
import com.linkups.api.mapper.ActivityMapper;
import com.linkups.domain.entity.Activity;
import com.linkups.domain.exception.ValidationException;
import com.linkups.domain.service.ActivityService;
import jakarta.validation.Valid;
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
    public ResponseEntity<ActivityFeedResponseDTO> getActivityFeed(
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
        ActivityFeedResponseDTO response = ActivityMapper.toActivityFeedResponse(activities);

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
    public ResponseEntity<ActivityFeedResponseDTO> getFriendActivities(
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
        ActivityFeedResponseDTO response = ActivityMapper.toActivityFeedResponse(activities);

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
    public ResponseEntity<SuccessResponseDTO> markActivityAsRead(
            @PathVariable Long activityId,
            @Valid @RequestBody MarkActivityReadRequestDTO request) {

        log.info("Marking activity {} as read", activityId);

        Activity activity = activityService.markActivityAsRead(activityId, request.getUserId());

        SuccessResponseDTO response = SuccessResponseDTO.of("Activity marked as read");

        log.info("Activity {} marked as read by user {}", activityId, request.getUserId());
        return ResponseEntity.ok(response);
    }

    /**
     * Get activity statistics for a user
     *
     * @param userId User ID
     * @return Activity statistics
     */
    @GetMapping("/{userId}/stats")
    public ResponseEntity<ActivityStatisticsDTO> getActivityStatistics(@PathVariable Long userId) {
        log.info("Getting activity statistics for user {}", userId);

        ActivityService.ActivityStatistics stats = activityService.getActivityStatistics(userId);
        ActivityStatisticsDTO response = ActivityMapper.toActivityStatistics(stats);

        return ResponseEntity.ok(response);
    }

    /**
     * Create a new activity (for testing or integration purposes)
     *
     * @param request Activity creation request
     * @return Created activity
     */
    @PostMapping
    public ResponseEntity<ActivityResponseDTO> createActivity(@Valid @RequestBody CreateActivityRequestDTO request) {
        log.info("Creating activity for user {} with type {}", request.getUserId(), request.getType());

        Activity activity = activityService.createActivity(
            request.getUserId(),
            request.getType(),
            request.getData(),
            request.getPriority(),
            request.getVisibility(),
            request.getRelatedEntityId(),
            request.getRelatedEntityType()
        );

        ActivityResponseDTO response = ActivityMapper.toActivityResponse(activity);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
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

}