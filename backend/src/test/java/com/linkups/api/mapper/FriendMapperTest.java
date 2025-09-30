package com.linkups.api.mapper;

import com.linkups.api.dto.response.friend.FriendListResponseDTO;
import com.linkups.api.dto.response.friend.FriendResponseDTO;
import com.linkups.api.dto.response.friend.FriendshipStatisticsDTO;
import com.linkups.base.BaseUnitTest;
import com.linkups.domain.entity.Friend;
import com.linkups.domain.entity.User;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

class FriendMapperTest extends BaseUnitTest {

    @Test
    void toFriendResponse_withValidFriend_shouldMapCorrectly() {
        // Given
        Friend friend = createFriend(1L, 100L, 200L, "ACCEPTED");

        // When
        FriendResponseDTO result = FriendMapper.toFriendResponse(friend);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getUserId()).isEqualTo(100L);
        assertThat(result.getFriendId()).isEqualTo(200L);
        assertThat(result.getStatus()).isEqualTo("ACCEPTED");
        assertThat(result.isAccepted()).isTrue();
        assertThat(result.isPending()).isFalse();
        assertThat(result.isRejected()).isFalse();
    }

    @Test
    void toFriendResponse_withNullFriend_shouldReturnNull() {
        // When
        FriendResponseDTO result = FriendMapper.toFriendResponse(null);

        // Then
        assertThat(result).isNull();
    }

    @Test
    void toFriendResponse_withPendingStatus_shouldSetHelperMethodsCorrectly() {
        // Given
        Friend friend = createFriend(1L, 100L, 200L, "PENDING");

        // When
        FriendResponseDTO result = FriendMapper.toFriendResponse(friend);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo("PENDING");
        assertThat(result.isPending()).isTrue();
        assertThat(result.isAccepted()).isFalse();
        assertThat(result.isRejected()).isFalse();
    }

    @Test
    void toFriendResponse_withRejectedStatus_shouldSetHelperMethodsCorrectly() {
        // Given
        Friend friend = createFriend(1L, 100L, 200L, "REJECTED");

        // When
        FriendResponseDTO result = FriendMapper.toFriendResponse(friend);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo("REJECTED");
        assertThat(result.isRejected()).isTrue();
        assertThat(result.isPending()).isFalse();
        assertThat(result.isAccepted()).isFalse();
    }

    @Test
    void toFriendResponse_withUserEntity_shouldMapCorrectly() {
        // Given
        User user = createUser(100L, "John Doe", "john@test.com");
        Long requestingUserId = 200L;

        // When
        FriendResponseDTO result = FriendMapper.toFriendResponse(user, requestingUserId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getUserId()).isEqualTo(200L);
        assertThat(result.getFriendId()).isEqualTo(100L);
        assertThat(result.getFriendName()).isEqualTo("John Doe");
        assertThat(result.getFriendEmail()).isEqualTo("john@test.com");
        assertThat(result.getStatus()).isEqualTo("ACCEPTED");
    }

    @Test
    void toFriendResponse_withNullUser_shouldReturnNull() {
        // When
        FriendResponseDTO result = FriendMapper.toFriendResponse(null, 100L);

        // Then
        assertThat(result).isNull();
    }

    @Test
    void toFriendResponseList_withValidFriends_shouldMapCorrectly() {
        // Given
        List<Friend> friends = Arrays.asList(
                createFriend(1L, 100L, 200L, "ACCEPTED"),
                createFriend(2L, 100L, 201L, "PENDING"),
                createFriend(3L, 100L, 202L, "ACCEPTED")
        );

        // When
        List<FriendResponseDTO> result = FriendMapper.toFriendResponseList(friends);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(3);
        assertThat(result.get(0).getStatus()).isEqualTo("ACCEPTED");
        assertThat(result.get(1).getStatus()).isEqualTo("PENDING");
        assertThat(result.get(2).getStatus()).isEqualTo("ACCEPTED");
    }

    @Test
    void toFriendResponseList_withEmptyList_shouldReturnEmptyList() {
        // When
        List<FriendResponseDTO> result = FriendMapper.toFriendResponseList(Collections.emptyList());

        // Then
        assertThat(result).isNotNull();
        assertThat(result).isEmpty();
    }

    @Test
    void toFriendResponseList_withNullList_shouldReturnEmptyList() {
        // When
        List<FriendResponseDTO> result = FriendMapper.toFriendResponseList(null);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).isEmpty();
    }

    @Test
    void toFriendResponseListFromUsers_withValidUsers_shouldMapCorrectly() {
        // Given
        List<User> users = Arrays.asList(
                createUser(100L, "Alice", "alice@test.com"),
                createUser(101L, "Bob", "bob@test.com"),
                createUser(102L, "Charlie", "charlie@test.com")
        );
        Long requestingUserId = 200L;

        // When
        List<FriendResponseDTO> result = FriendMapper.toFriendResponseListFromUsers(users, requestingUserId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(3);
        assertThat(result.get(0).getFriendName()).isEqualTo("Alice");
        assertThat(result.get(1).getFriendName()).isEqualTo("Bob");
        assertThat(result.get(2).getFriendName()).isEqualTo("Charlie");
        assertThat(result.get(0).getUserId()).isEqualTo(requestingUserId);
    }

    @Test
    void toFriendResponseListFromUsers_withEmptyList_shouldReturnEmptyList() {
        // When
        List<FriendResponseDTO> result = FriendMapper.toFriendResponseListFromUsers(Collections.emptyList(), 100L);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).isEmpty();
    }

    @Test
    void toFriendResponseListFromUsers_withNullList_shouldReturnEmptyList() {
        // When
        List<FriendResponseDTO> result = FriendMapper.toFriendResponseListFromUsers(null, 100L);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).isEmpty();
    }

    @Test
    void toFriendListResponse_withValidData_shouldMapCorrectly() {
        // Given
        List<FriendResponseDTO> friends = Arrays.asList(
                FriendResponseDTO.builder().id(1L).userId(100L).friendId(200L).status("ACCEPTED").build(),
                FriendResponseDTO.builder().id(2L).userId(100L).friendId(201L).status("ACCEPTED").build()
        );
        int totalCount = 10;
        int page = 2;
        int limit = 5;
        int totalPages = 2;

        // When
        FriendListResponseDTO result = FriendMapper.toFriendListResponse(friends, totalCount, page, limit, totalPages);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getFriends()).hasSize(2);
        assertThat(result.getTotalCount()).isEqualTo(10);
        assertThat(result.getPage()).isEqualTo(2);
        assertThat(result.getLimit()).isEqualTo(5);
        assertThat(result.getTotalPages()).isEqualTo(2);
    }

    @Test
    void toFriendListResponse_withEmptyFriendsList_shouldMapCorrectly() {
        // When
        FriendListResponseDTO result = FriendMapper.toFriendListResponse(Collections.emptyList(), 0, 1, 10, 0);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getFriends()).isEmpty();
        assertThat(result.getTotalCount()).isEqualTo(0);
        assertThat(result.getTotalPages()).isEqualTo(0);
    }

    @Test
    void toFriendshipStatistics_withValidStats_shouldMapCorrectly() {
        // Given
        Map<String, Long> stats = new HashMap<>();
        stats.put("totalFriends", 50L);
        stats.put("pendingRequests", 5L);
        stats.put("sentRequests", 3L);
        stats.put("mutualFriends", 15L);

        // When
        FriendshipStatisticsDTO result = FriendMapper.toFriendshipStatistics(stats);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getTotalFriends()).isEqualTo(50L);
        assertThat(result.getPendingRequests()).isEqualTo(5L);
        assertThat(result.getSentRequests()).isEqualTo(3L);
        assertThat(result.getMutualFriends()).isEqualTo(15L);
    }

    @Test
    void toFriendshipStatistics_withNullStats_shouldReturnDefaultValues() {
        // When
        FriendshipStatisticsDTO result = FriendMapper.toFriendshipStatistics(null);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getTotalFriends()).isEqualTo(0L);
        assertThat(result.getPendingRequests()).isEqualTo(0L);
        assertThat(result.getSentRequests()).isEqualTo(0L);
        assertThat(result.getMutualFriends()).isEqualTo(0L);
    }

    @Test
    void toFriendshipStatistics_withEmptyStats_shouldReturnDefaultValues() {
        // When
        FriendshipStatisticsDTO result = FriendMapper.toFriendshipStatistics(Collections.emptyMap());

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getTotalFriends()).isEqualTo(0L);
        assertThat(result.getPendingRequests()).isEqualTo(0L);
        assertThat(result.getSentRequests()).isEqualTo(0L);
        assertThat(result.getMutualFriends()).isEqualTo(0L);
    }

    @Test
    void toFriendshipStatistics_withPartialStats_shouldUseDefaultForMissingValues() {
        // Given
        Map<String, Long> stats = new HashMap<>();
        stats.put("totalFriends", 25L);
        stats.put("pendingRequests", 2L);
        // Missing sentRequests and mutualFriends

        // When
        FriendshipStatisticsDTO result = FriendMapper.toFriendshipStatistics(stats);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getTotalFriends()).isEqualTo(25L);
        assertThat(result.getPendingRequests()).isEqualTo(2L);
        assertThat(result.getSentRequests()).isEqualTo(0L);
        assertThat(result.getMutualFriends()).isEqualTo(0L);
    }

    // Helper methods
    private Friend createFriend(Long id, Long userId, Long friendId, String status) {
        Friend friend = Friend.builder()
                .id(id)
                .userId(userId)
                .friendId(friendId)
                .status(status)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        // Set up User entities if needed
        User user = createUser(userId, "User " + userId, "user" + userId + "@test.com");
        User friendUser = createUser(friendId, "Friend " + friendId, "friend" + friendId + "@test.com");
        friend.setUser(user);
        friend.setFriend(friendUser);

        return friend;
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