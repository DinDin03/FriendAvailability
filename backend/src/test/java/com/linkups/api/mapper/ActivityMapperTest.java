package com.linkups.api.mapper;

import com.linkups.api.dto.response.activity.ActivityFeedResponseDTO;
import com.linkups.api.dto.response.activity.ActivityResponseDTO;
import com.linkups.api.dto.response.activity.ActivityStatisticsDTO;
import com.linkups.base.BaseUnitTest;
import com.linkups.domain.entity.Activity;
import com.linkups.domain.service.ActivityService;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ActivityMapperTest extends BaseUnitTest {

    @Test
    void toActivityResponse_withValidActivity_shouldMapCorrectly() {
        // Given
        Activity activity = createActivity(1L, "availability_change", 100L,
            "{\"status\":\"available\",\"activity\":\"studying\"}", 3);

        // When
        ActivityResponseDTO result = ActivityMapper.toActivityResponse(activity);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getType()).isEqualTo("availability_change");
        assertThat(result.getUserId()).isEqualTo(100L);
        assertThat(result.getData()).isEqualTo("{\"status\":\"available\",\"activity\":\"studying\"}");
        assertThat(result.getPriority()).isEqualTo(3);
        assertThat(result.getVisibility()).isEqualTo("friends");
        assertThat(result.getIsActive()).isTrue();
        assertThat(result.isHighPriority()).isFalse();
    }

    @Test
    void toActivityResponse_withNullActivity_shouldReturnNull() {
        // When
        ActivityResponseDTO result = ActivityMapper.toActivityResponse(null);

        // Then
        assertThat(result).isNull();
    }

    @Test
    void toActivityResponse_withHighPriorityActivity_shouldSetHelperMethodCorrectly() {
        // Given
        Activity activity = createActivity(1L, "urgent_update", 100L, "{}", 5);

        // When
        ActivityResponseDTO result = ActivityMapper.toActivityResponse(activity);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getPriority()).isEqualTo(5);
        assertThat(result.isHighPriority()).isTrue();
    }

    @Test
    void toActivityResponse_withPriority4_shouldBeHighPriority() {
        // Given
        Activity activity = createActivity(1L, "important_update", 100L, "{}", 4);

        // When
        ActivityResponseDTO result = ActivityMapper.toActivityResponse(activity);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getPriority()).isEqualTo(4);
        assertThat(result.isHighPriority()).isTrue();
    }

    @Test
    void toActivityResponse_withRelatedEntity_shouldMapCorrectly() {
        // Given
        Activity activity = createActivityWithRelatedEntity(1L, "circle_activity", 100L,
            "{\"action\":\"joined\"}", 2, 50L, "circle");

        // When
        ActivityResponseDTO result = ActivityMapper.toActivityResponse(activity);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getRelatedEntityId()).isEqualTo(50L);
        assertThat(result.getRelatedEntityType()).isEqualTo("circle");
    }

    @Test
    void toActivityResponseList_withValidActivities_shouldMapCorrectly() {
        // Given
        List<Activity> activities = Arrays.asList(
                createActivity(1L, "profile_update", 100L, "{}", 1),
                createActivity(2L, "availability_change", 101L, "{}", 2),
                createActivity(3L, "social_activity", 102L, "{}", 3)
        );

        // When
        List<ActivityResponseDTO> result = ActivityMapper.toActivityResponseList(activities);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(3);
        assertThat(result.get(0).getType()).isEqualTo("profile_update");
        assertThat(result.get(1).getType()).isEqualTo("availability_change");
        assertThat(result.get(2).getType()).isEqualTo("social_activity");
    }

    @Test
    void toActivityResponseList_withEmptyList_shouldReturnEmptyList() {
        // When
        List<ActivityResponseDTO> result = ActivityMapper.toActivityResponseList(Collections.emptyList());

        // Then
        assertThat(result).isNotNull();
        assertThat(result).isEmpty();
    }

    @Test
    void toActivityResponseList_withNullList_shouldReturnEmptyList() {
        // When
        List<ActivityResponseDTO> result = ActivityMapper.toActivityResponseList(null);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).isEmpty();
    }

    @Test
    void toActivityFeedResponse_withPagedActivities_shouldMapCorrectly() {
        // Given
        List<Activity> activities = Arrays.asList(
                createActivity(1L, "activity_1", 100L, "{}", 1),
                createActivity(2L, "activity_2", 101L, "{}", 2)
        );
        Page<Activity> page = new PageImpl<>(activities, PageRequest.of(0, 10), 20);

        // When
        ActivityFeedResponseDTO result = ActivityMapper.toActivityFeedResponse(page);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getActivities()).hasSize(2);
        assertThat(result.getTotalCount()).isEqualTo(20L);
        assertThat(result.getHasMore()).isTrue();
        assertThat(result.getNextCursor()).isNotNull();
    }

    @Test
    void toActivityFeedResponse_withLastPage_shouldNotHaveNextCursor() {
        // Given
        List<Activity> activities = Arrays.asList(
                createActivity(1L, "activity_1", 100L, "{}", 1)
        );
        Page<Activity> page = new PageImpl<>(activities, PageRequest.of(1, 10), 11);

        // When
        ActivityFeedResponseDTO result = ActivityMapper.toActivityFeedResponse(page);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getActivities()).hasSize(1);
        assertThat(result.getHasMore()).isFalse();
        assertThat(result.getNextCursor()).isNull();
    }

    @Test
    void toActivityFeedResponse_withEmptyPage_shouldReturnEmptyResponse() {
        // Given
        Page<Activity> emptyPage = new PageImpl<>(Collections.emptyList());

        // When
        ActivityFeedResponseDTO result = ActivityMapper.toActivityFeedResponse(emptyPage);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getActivities()).isEmpty();
        assertThat(result.getTotalCount()).isEqualTo(0L);
        assertThat(result.getHasMore()).isFalse();
        assertThat(result.getNextCursor()).isNull();
    }

    @Test
    void toActivityFeedResponse_withNullPage_shouldReturnDefaultResponse() {
        // When
        ActivityFeedResponseDTO result = ActivityMapper.toActivityFeedResponse(null);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getActivities()).isEmpty();
        assertThat(result.getTotalCount()).isEqualTo(0L);
        assertThat(result.getHasMore()).isFalse();
        assertThat(result.getNextCursor()).isNull();
    }

    @Test
    void toActivityStatistics_withValidStatistics_shouldMapCorrectly() {
        // Given
        LocalDateTime lastUpdated = LocalDateTime.now();
        ActivityService.ActivityStatistics stats = new ActivityService.ActivityStatistics(
                150L, 10L, 45L, lastUpdated
        );

        // When
        ActivityStatisticsDTO result = ActivityMapper.toActivityStatistics(stats);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getTotalActivities()).isEqualTo(150L);
        assertThat(result.getTodayActivities()).isEqualTo(10L);
        assertThat(result.getWeekActivities()).isEqualTo(45L);
        assertThat(result.getLastUpdated()).isNotNull();
    }

    @Test
    void toActivityStatistics_withNullStatistics_shouldReturnDefaultValues() {
        // When
        ActivityStatisticsDTO result = ActivityMapper.toActivityStatistics(null);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getTotalActivities()).isEqualTo(0L);
        assertThat(result.getTodayActivities()).isEqualTo(0L);
        assertThat(result.getWeekActivities()).isEqualTo(0L);
        assertThat(result.getLastUpdated()).isNull();
    }

    @Test
    void toActivityStatistics_withNullLastUpdated_shouldMapCorrectly() {
        // Given
        ActivityService.ActivityStatistics stats = new ActivityService.ActivityStatistics(
                100L, 5L, 20L, null
        );

        // When
        ActivityStatisticsDTO result = ActivityMapper.toActivityStatistics(stats);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getTotalActivities()).isEqualTo(100L);
        assertThat(result.getTodayActivities()).isEqualTo(5L);
        assertThat(result.getWeekActivities()).isEqualTo(20L);
        assertThat(result.getLastUpdated()).isNull();
    }

    @Test
    void toActivityResponse_withAllVisibilityLevels_shouldMapCorrectly() {
        // Test public visibility
        Activity publicActivity = createActivity(1L, "test", 100L, "{}", 1);
        publicActivity.setVisibility("public");
        ActivityResponseDTO publicResult = ActivityMapper.toActivityResponse(publicActivity);
        assertThat(publicResult.getVisibility()).isEqualTo("public");

        // Test friends visibility
        Activity friendsActivity = createActivity(2L, "test", 100L, "{}", 1);
        friendsActivity.setVisibility("friends");
        ActivityResponseDTO friendsResult = ActivityMapper.toActivityResponse(friendsActivity);
        assertThat(friendsResult.getVisibility()).isEqualTo("friends");

        // Test private visibility
        Activity privateActivity = createActivity(3L, "test", 100L, "{}", 1);
        privateActivity.setVisibility("private");
        ActivityResponseDTO privateResult = ActivityMapper.toActivityResponse(privateActivity);
        assertThat(privateResult.getVisibility()).isEqualTo("private");
    }

    @Test
    void toActivityResponse_withInactiveActivity_shouldMapCorrectly() {
        // Given
        Activity activity = createActivity(1L, "old_activity", 100L, "{}", 1);
        activity.setIsActive(false);

        // When
        ActivityResponseDTO result = ActivityMapper.toActivityResponse(activity);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getIsActive()).isFalse();
    }

    // Helper methods
    private Activity createActivity(Long id, String type, Long userId, String data, Integer priority) {
        LocalDateTime now = LocalDateTime.now();
        return Activity.builder()
                .id(id)
                .type(type)
                .userId(userId)
                .timestamp(now)
                .data(data)
                .priority(priority)
                .visibility("friends")
                .isActive(true)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    private Activity createActivityWithRelatedEntity(Long id, String type, Long userId,
                                                     String data, Integer priority,
                                                     Long relatedEntityId, String relatedEntityType) {
        Activity activity = createActivity(id, type, userId, data, priority);
        activity.setRelatedEntityId(relatedEntityId);
        activity.setRelatedEntityType(relatedEntityType);
        return activity;
    }
}