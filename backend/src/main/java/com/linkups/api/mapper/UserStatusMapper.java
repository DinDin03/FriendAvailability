package com.linkups.api.mapper;

import com.linkups.api.dto.response.userstatus.BatchUserStatusResponseDTO;
import com.linkups.api.dto.response.userstatus.StatusStatisticsDTO;
import com.linkups.api.dto.response.userstatus.UserStatusResponseDTO;
import com.linkups.domain.entity.User;
import com.linkups.domain.entity.UserStatus;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * UserStatus Mapper
 *
 * Utility class for converting between UserStatus domain entities and DTOs.
 */
public class UserStatusMapper {

    private UserStatusMapper() {
        throw new UnsupportedOperationException("UserStatusMapper is a utility class");
    }

    // ========== USER STATUS CONVERSIONS ==========

    public static UserStatusResponseDTO toUserStatusResponse(UserStatus userStatus) {
        if (userStatus == null) return null;

        return UserStatusResponseDTO.builder()
                .id(userStatus.getId())
                .userId(userStatus.getUserId())
                .userName(null) // Can be enriched by service layer
                .status(userStatus.getStatus() != null ? userStatus.getStatus().toString() : "OFFLINE")
                .lastSeen(userStatus.getLastSeen())
                .currentActivity(userStatus.getCurrentActivity())
                .updatedAt(userStatus.getUpdatedAt())
                .build();
    }

    public static UserStatusResponseDTO toUserStatusResponse(UserStatus userStatus, User user) {
        if (userStatus == null) return null;

        UserStatusResponseDTO dto = toUserStatusResponse(userStatus);
        if (user != null) {
            dto.setUserName(user.getName());
        }
        return dto;
    }

    public static List<UserStatusResponseDTO> toUserStatusResponseList(List<UserStatus> statuses) {
        if (statuses == null || statuses.isEmpty()) {
            return Collections.emptyList();
        }

        return statuses.stream()
                .map(UserStatusMapper::toUserStatusResponse)
                .collect(Collectors.toList());
    }

    public static BatchUserStatusResponseDTO toBatchUserStatusResponse(List<UserStatus> statuses) {
        if (statuses == null || statuses.isEmpty()) {
            return BatchUserStatusResponseDTO.builder()
                    .statuses(Collections.emptyList())
                    .count(0)
                    .build();
        }

        List<UserStatusResponseDTO> statusList = toUserStatusResponseList(statuses);

        return BatchUserStatusResponseDTO.builder()
                .statuses(statusList)
                .count(statusList.size())
                .build();
    }

    public static StatusStatisticsDTO toStatusStatistics(Map<String, Long> stats) {
        if (stats == null) {
            return StatusStatisticsDTO.builder()
                    .onlineCount(0L)
                    .awayCount(0L)
                    .busyCount(0L)
                    .offlineCount(0L)
                    .totalUsers(0L)
                    .build();
        }

        return StatusStatisticsDTO.builder()
                .onlineCount(stats.getOrDefault("ONLINE", 0L))
                .awayCount(stats.getOrDefault("AWAY", 0L))
                .busyCount(stats.getOrDefault("BUSY", 0L))
                .offlineCount(stats.getOrDefault("OFFLINE", 0L))
                .totalUsers(stats.getOrDefault("TOTAL", 0L))
                .build();
    }
}