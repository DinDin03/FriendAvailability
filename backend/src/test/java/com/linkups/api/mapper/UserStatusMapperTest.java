package com.linkups.api.mapper;

import com.linkups.api.dto.response.userstatus.BatchUserStatusResponseDTO;
import com.linkups.api.dto.response.userstatus.StatusStatisticsDTO;
import com.linkups.api.dto.response.userstatus.UserStatusResponseDTO;
import com.linkups.base.BaseUnitTest;
import com.linkups.domain.entity.User;
import com.linkups.domain.entity.UserStatus;
import com.linkups.domain.entity.enums.UserStatusType;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

class UserStatusMapperTest extends BaseUnitTest {

    @Test
    void toUserStatusResponse_withValidUserStatus_shouldMapCorrectly() {
        // Given
        UserStatus userStatus = createUserStatus(1L, 100L, UserStatusType.ONLINE, "Working on project");

        // When
        UserStatusResponseDTO result = UserStatusMapper.toUserStatusResponse(userStatus);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getUserId()).isEqualTo(100L);
        assertThat(result.getStatus()).isEqualTo("ONLINE");
        assertThat(result.getCurrentActivity()).isEqualTo("Working on project");
        assertThat(result.isOnline()).isTrue();
        assertThat(result.isOffline()).isFalse();
        assertThat(result.isAway()).isFalse();
        assertThat(result.isBusy()).isFalse();
    }

    @Test
    void toUserStatusResponse_withNullUserStatus_shouldReturnNull() {
        // When
        UserStatusResponseDTO result = UserStatusMapper.toUserStatusResponse(null);

        // Then
        assertThat(result).isNull();
    }

    @Test
    void toUserStatusResponse_withNullStatus_shouldDefaultToOffline() {
        // Given
        UserStatus userStatus = createUserStatus(1L, 100L, null, null);

        // When
        UserStatusResponseDTO result = UserStatusMapper.toUserStatusResponse(userStatus);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo("OFFLINE");
        assertThat(result.isOffline()).isTrue();
    }

    @Test
    void toUserStatusResponse_withOfflineStatus_shouldSetHelperMethodCorrectly() {
        // Given
        UserStatus userStatus = createUserStatus(1L, 100L, UserStatusType.OFFLINE, null);

        // When
        UserStatusResponseDTO result = UserStatusMapper.toUserStatusResponse(userStatus);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo("OFFLINE");
        assertThat(result.isOffline()).isTrue();
        assertThat(result.isOnline()).isFalse();
        assertThat(result.isAway()).isFalse();
        assertThat(result.isBusy()).isFalse();
    }

    @Test
    void toUserStatusResponse_withAwayStatus_shouldSetHelperMethodCorrectly() {
        // Given
        UserStatus userStatus = createUserStatus(1L, 100L, UserStatusType.AWAY, "Away from desk");

        // When
        UserStatusResponseDTO result = UserStatusMapper.toUserStatusResponse(userStatus);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo("AWAY");
        assertThat(result.isAway()).isTrue();
        assertThat(result.isOnline()).isFalse();
        assertThat(result.isOffline()).isFalse();
        assertThat(result.isBusy()).isFalse();
    }

    @Test
    void toUserStatusResponse_withBusyStatus_shouldSetHelperMethodCorrectly() {
        // Given
        UserStatus userStatus = createUserStatus(1L, 100L, UserStatusType.BUSY, "In a meeting");

        // When
        UserStatusResponseDTO result = UserStatusMapper.toUserStatusResponse(userStatus);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo("BUSY");
        assertThat(result.isBusy()).isTrue();
        assertThat(result.isOnline()).isFalse();
        assertThat(result.isOffline()).isFalse();
        assertThat(result.isAway()).isFalse();
    }

    @Test
    void toUserStatusResponse_withDoNotDisturbStatus_shouldMapCorrectly() {
        // Given
        UserStatus userStatus = createUserStatus(1L, 100L, UserStatusType.DO_NOT_DISTURB, "Focus time");

        // When
        UserStatusResponseDTO result = UserStatusMapper.toUserStatusResponse(userStatus);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo("DO_NOT_DISTURB");
        assertThat(result.getCurrentActivity()).isEqualTo("Focus time");
    }

    @Test
    void toUserStatusResponse_withUserAndStatus_shouldEnrichWithUserName() {
        // Given
        UserStatus userStatus = createUserStatus(1L, 100L, UserStatusType.ONLINE, "Studying");
        User user = createUser(100L, "John Doe", "john@test.com");

        // When
        UserStatusResponseDTO result = UserStatusMapper.toUserStatusResponse(userStatus, user);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getUserId()).isEqualTo(100L);
        assertThat(result.getUserName()).isEqualTo("John Doe");
        assertThat(result.getStatus()).isEqualTo("ONLINE");
    }

    @Test
    void toUserStatusResponse_withNullUser_shouldNotSetUserName() {
        // Given
        UserStatus userStatus = createUserStatus(1L, 100L, UserStatusType.ONLINE, "Studying");

        // When
        UserStatusResponseDTO result = UserStatusMapper.toUserStatusResponse(userStatus, null);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getUserId()).isEqualTo(100L);
        assertThat(result.getUserName()).isNull();
    }

    @Test
    void toUserStatusResponseList_withValidStatuses_shouldMapCorrectly() {
        // Given
        List<UserStatus> statuses = Arrays.asList(
                createUserStatus(1L, 100L, UserStatusType.ONLINE, "Working"),
                createUserStatus(2L, 101L, UserStatusType.AWAY, "Lunch break"),
                createUserStatus(3L, 102L, UserStatusType.BUSY, "Meeting")
        );

        // When
        List<UserStatusResponseDTO> result = UserStatusMapper.toUserStatusResponseList(statuses);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(3);
        assertThat(result.get(0).getStatus()).isEqualTo("ONLINE");
        assertThat(result.get(1).getStatus()).isEqualTo("AWAY");
        assertThat(result.get(2).getStatus()).isEqualTo("BUSY");
    }

    @Test
    void toUserStatusResponseList_withEmptyList_shouldReturnEmptyList() {
        // When
        List<UserStatusResponseDTO> result = UserStatusMapper.toUserStatusResponseList(Collections.emptyList());

        // Then
        assertThat(result).isNotNull();
        assertThat(result).isEmpty();
    }

    @Test
    void toUserStatusResponseList_withNullList_shouldReturnEmptyList() {
        // When
        List<UserStatusResponseDTO> result = UserStatusMapper.toUserStatusResponseList(null);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).isEmpty();
    }

    @Test
    void toBatchUserStatusResponse_withValidStatuses_shouldMapCorrectly() {
        // Given
        List<UserStatus> statuses = Arrays.asList(
                createUserStatus(1L, 100L, UserStatusType.ONLINE, "Available"),
                createUserStatus(2L, 101L, UserStatusType.OFFLINE, null)
        );

        // When
        BatchUserStatusResponseDTO result = UserStatusMapper.toBatchUserStatusResponse(statuses);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getStatuses()).hasSize(2);
        assertThat(result.getCount()).isEqualTo(2);
    }

    @Test
    void toBatchUserStatusResponse_withEmptyList_shouldReturnEmptyResponse() {
        // When
        BatchUserStatusResponseDTO result = UserStatusMapper.toBatchUserStatusResponse(Collections.emptyList());

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getStatuses()).isEmpty();
        assertThat(result.getCount()).isEqualTo(0);
    }

    @Test
    void toBatchUserStatusResponse_withNullList_shouldReturnEmptyResponse() {
        // When
        BatchUserStatusResponseDTO result = UserStatusMapper.toBatchUserStatusResponse(null);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getStatuses()).isEmpty();
        assertThat(result.getCount()).isEqualTo(0);
    }

    @Test
    void toStatusStatistics_withValidStats_shouldMapCorrectly() {
        // Given
        Map<String, Long> stats = new HashMap<>();
        stats.put("ONLINE", 50L);
        stats.put("AWAY", 15L);
        stats.put("BUSY", 10L);
        stats.put("OFFLINE", 125L);
        stats.put("TOTAL", 200L);

        // When
        StatusStatisticsDTO result = UserStatusMapper.toStatusStatistics(stats);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getOnlineCount()).isEqualTo(50L);
        assertThat(result.getAwayCount()).isEqualTo(15L);
        assertThat(result.getBusyCount()).isEqualTo(10L);
        assertThat(result.getOfflineCount()).isEqualTo(125L);
        assertThat(result.getTotalUsers()).isEqualTo(200L);
    }

    @Test
    void toStatusStatistics_withNullStats_shouldReturnDefaultValues() {
        // When
        StatusStatisticsDTO result = UserStatusMapper.toStatusStatistics(null);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getOnlineCount()).isEqualTo(0L);
        assertThat(result.getAwayCount()).isEqualTo(0L);
        assertThat(result.getBusyCount()).isEqualTo(0L);
        assertThat(result.getOfflineCount()).isEqualTo(0L);
        assertThat(result.getTotalUsers()).isEqualTo(0L);
    }

    @Test
    void toStatusStatistics_withEmptyStats_shouldReturnDefaultValues() {
        // When
        StatusStatisticsDTO result = UserStatusMapper.toStatusStatistics(Collections.emptyMap());

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getOnlineCount()).isEqualTo(0L);
        assertThat(result.getAwayCount()).isEqualTo(0L);
        assertThat(result.getBusyCount()).isEqualTo(0L);
        assertThat(result.getOfflineCount()).isEqualTo(0L);
        assertThat(result.getTotalUsers()).isEqualTo(0L);
    }

    @Test
    void toStatusStatistics_withPartialStats_shouldUseDefaultForMissingValues() {
        // Given
        Map<String, Long> stats = new HashMap<>();
        stats.put("ONLINE", 30L);
        stats.put("AWAY", 5L);
        // Missing BUSY, OFFLINE, and TOTAL

        // When
        StatusStatisticsDTO result = UserStatusMapper.toStatusStatistics(stats);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getOnlineCount()).isEqualTo(30L);
        assertThat(result.getAwayCount()).isEqualTo(5L);
        assertThat(result.getBusyCount()).isEqualTo(0L);
        assertThat(result.getOfflineCount()).isEqualTo(0L);
        assertThat(result.getTotalUsers()).isEqualTo(0L);
    }

    @Test
    void toUserStatusResponse_withAllStatusTypes_shouldMapCorrectly() {
        // Test all UserStatusType enum values
        for (UserStatusType statusType : UserStatusType.values()) {
            UserStatus userStatus = createUserStatus(1L, 100L, statusType, "Test activity");
            UserStatusResponseDTO result = UserStatusMapper.toUserStatusResponse(userStatus);

            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(statusType.toString());
        }
    }

    @Test
    void toUserStatusResponse_withNullActivity_shouldMapCorrectly() {
        // Given
        UserStatus userStatus = createUserStatus(1L, 100L, UserStatusType.ONLINE, null);

        // When
        UserStatusResponseDTO result = UserStatusMapper.toUserStatusResponse(userStatus);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getCurrentActivity()).isNull();
    }

    @Test
    void toUserStatusResponse_shouldIncludeTimestamps() {
        // Given
        LocalDateTime now = LocalDateTime.now();
        UserStatus userStatus = createUserStatus(1L, 100L, UserStatusType.ONLINE, "Working");

        // When
        UserStatusResponseDTO result = UserStatusMapper.toUserStatusResponse(userStatus);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getLastSeen()).isNotNull();
        assertThat(result.getUpdatedAt()).isNotNull();
    }

    // Helper methods
    private UserStatus createUserStatus(Long id, Long userId, UserStatusType status, String currentActivity) {
        LocalDateTime now = LocalDateTime.now();
        return UserStatus.builder()
                .id(id)
                .userId(userId)
                .status(status != null ? status : UserStatusType.OFFLINE)
                .lastSeen(now)
                .currentActivity(currentActivity)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    private User createUser(Long id, String name, String email) {
        return User.builder()
                .id(id)
                .name(name)
                .email(email)
                .isActive(true)
                .emailVerified(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }
}