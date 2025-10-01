package com.linkups.api.mapper;

import com.linkups.api.dto.response.friend.FriendListResponseDTO;
import com.linkups.api.dto.response.friend.FriendResponseDTO;
import com.linkups.api.dto.response.friend.FriendshipStatisticsDTO;
import com.linkups.domain.entity.Friend;
import com.linkups.domain.entity.User;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Friend Mapper
 *
 * Utility class for converting between Friend domain entities and DTOs.
 */
public class FriendMapper {

    private FriendMapper() {
        throw new UnsupportedOperationException("FriendMapper is a utility class");
    }

    // ========== FRIEND CONVERSIONS ==========

    public static FriendResponseDTO toFriendResponse(Friend friend) {
        if (friend == null) return null;

        return FriendResponseDTO.builder()
                .id(friend.getId())
                .userId(friend.getUserId())
                .friendId(friend.getFriendId())
                .friendName(friend.getFriend() != null ? friend.getFriend().getName() : null)
                .friendEmail(friend.getFriend() != null ? friend.getFriend().getEmail() : null)
                .status(friend.getStatus())
                .createdAt(friend.getCreatedAt())
                .updatedAt(friend.getUpdatedAt())
                .build();
    }

    /**
     * Convert Friend entity to FriendResponseDTO for pending requests
     * For pending requests, the sender is in the userId field
     */
    public static FriendResponseDTO toPendingRequestResponse(Friend friend) {
        if (friend == null) return null;

        return FriendResponseDTO.builder()
                .id(friend.getId())
                .userId(friend.getUserId())
                .friendId(friend.getFriendId())
                .friendName(friend.getUser() != null ? friend.getUser().getName() : null)
                .friendEmail(friend.getUser() != null ? friend.getUser().getEmail() : null)
                .status(friend.getStatus())
                .createdAt(friend.getCreatedAt())
                .updatedAt(friend.getUpdatedAt())
                .build();
    }

    public static FriendResponseDTO toFriendResponse(User user, Long currentUserId) {
        if (user == null) return null;

        return FriendResponseDTO.builder()
                .userId(currentUserId)
                .friendId(user.getId())
                .friendName(user.getName())
                .friendEmail(user.getEmail())
                .status("ACCEPTED") // Assuming already friends if converting from User
                .build();
    }

    public static List<FriendResponseDTO> toFriendResponseList(List<Friend> friends) {
        if (friends == null || friends.isEmpty()) {
            return Collections.emptyList();
        }

        return friends.stream()
                .map(FriendMapper::toFriendResponse)
                .collect(Collectors.toList());
    }

    public static List<FriendResponseDTO> toPendingRequestResponseList(List<Friend> friends) {
        if (friends == null || friends.isEmpty()) {
            return Collections.emptyList();
        }

        return friends.stream()
                .map(FriendMapper::toPendingRequestResponse)
                .collect(Collectors.toList());
    }

    public static List<FriendResponseDTO> toFriendResponseListFromUsers(List<User> users, Long currentUserId) {
        if (users == null || users.isEmpty()) {
            return Collections.emptyList();
        }

        return users.stream()
                .map(user -> toFriendResponse(user, currentUserId))
                .collect(Collectors.toList());
    }

    public static FriendListResponseDTO toFriendListResponse(
            List<FriendResponseDTO> friends,
            int totalCount,
            int page,
            int limit,
            int totalPages) {

        return FriendListResponseDTO.builder()
                .friends(friends)
                .totalCount(totalCount)
                .page(page)
                .limit(limit)
                .totalPages(totalPages)
                .build();
    }

    public static FriendshipStatisticsDTO toFriendshipStatistics(Map<String, Long> stats) {
        if (stats == null) {
            return FriendshipStatisticsDTO.builder()
                    .totalFriends(0L)
                    .pendingRequests(0L)
                    .sentRequests(0L)
                    .mutualFriends(0L)
                    .build();
        }

        return FriendshipStatisticsDTO.builder()
                .totalFriends(stats.getOrDefault("totalFriends", 0L))
                .pendingRequests(stats.getOrDefault("pendingRequests", 0L))
                .sentRequests(stats.getOrDefault("sentRequests", 0L))
                .mutualFriends(stats.getOrDefault("mutualFriends", 0L))
                .build();
    }
}