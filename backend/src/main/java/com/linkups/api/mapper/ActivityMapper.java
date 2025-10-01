package com.linkups.api.mapper;

import com.linkups.api.dto.response.activity.ActivityFeedResponseDTO;
import com.linkups.api.dto.response.activity.ActivityResponseDTO;
import com.linkups.api.dto.response.activity.ActivityStatisticsDTO;
import com.linkups.domain.entity.Activity;
import com.linkups.domain.service.ActivityService;
import org.springframework.data.domain.Page;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Activity Mapper
 *
 * Utility class for converting between Activity domain entities and DTOs.
 */
public class ActivityMapper {

    private ActivityMapper() {
        throw new UnsupportedOperationException("ActivityMapper is a utility class");
    }

    // ========== ACTIVITY CONVERSIONS ==========

    public static ActivityResponseDTO toActivityResponse(Activity activity) {
        if (activity == null) return null;

        return ActivityResponseDTO.builder()
                .id(activity.getId())
                .type(activity.getType())
                .userId(activity.getUserId())
                .userName(null) // Can be enriched by service layer
                .timestamp(activity.getTimestamp())
                .data(activity.getData())
                .priority(activity.getPriority())
                .visibility(activity.getVisibility())
                .isActive(activity.getIsActive())
                .relatedEntityId(activity.getRelatedEntityId())
                .relatedEntityType(activity.getRelatedEntityType())
                .createdAt(activity.getCreatedAt())
                .build();
    }

    public static List<ActivityResponseDTO> toActivityResponseList(List<Activity> activities) {
        if (activities == null || activities.isEmpty()) {
            return Collections.emptyList();
        }

        return activities.stream()
                .map(ActivityMapper::toActivityResponse)
                .collect(Collectors.toList());
    }

    public static ActivityFeedResponseDTO toActivityFeedResponse(Page<Activity> activities) {
        if (activities == null) {
            return ActivityFeedResponseDTO.builder()
                    .activities(Collections.emptyList())
                    .hasMore(false)
                    .nextCursor(null)
                    .totalCount(0L)
                    .build();
        }

        List<ActivityResponseDTO> activityList = toActivityResponseList(activities.getContent());
        String nextCursor = activities.hasNext() && !activityList.isEmpty()
                ? activityList.get(activityList.size() - 1).getTimestamp().toString()
                : null;

        return ActivityFeedResponseDTO.builder()
                .activities(activityList)
                .hasMore(activities.hasNext())
                .nextCursor(nextCursor)
                .totalCount(activities.getTotalElements())
                .build();
    }

    public static ActivityStatisticsDTO toActivityStatistics(ActivityService.ActivityStatistics stats) {
        if (stats == null) {
            return ActivityStatisticsDTO.builder()
                    .totalActivities(0L)
                    .todayActivities(0L)
                    .weekActivities(0L)
                    .lastUpdated(null)
                    .build();
        }

        return ActivityStatisticsDTO.builder()
                .totalActivities(stats.totalActivities)
                .todayActivities(stats.todayActivities)
                .weekActivities(stats.weekActivities)
                .lastUpdated(stats.lastUpdated != null ? stats.lastUpdated.toString() : null)
                .build();
    }
}