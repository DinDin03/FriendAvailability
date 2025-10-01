package com.linkups.api.controller.v1;

import com.linkups.api.dto.request.friend.SendFriendRequestDTO;
import com.linkups.api.dto.response.common.SuccessResponseDTO;
import com.linkups.api.dto.response.friend.FriendListResponseDTO;
import com.linkups.api.dto.response.friend.FriendResponseDTO;
import com.linkups.api.dto.response.friend.FriendshipStatisticsDTO;
import com.linkups.api.mapper.FriendMapper;
import com.linkups.domain.entity.Friend;
import com.linkups.domain.entity.User;
import com.linkups.domain.service.FriendService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/friends")
@Slf4j
public class FriendController {

    private final FriendService friendService;

    public FriendController(FriendService friendService) {
        this.friendService = friendService;
        log.info("FriendController initialized successfully");
    }

    @PostMapping("/request")
    public ResponseEntity<FriendResponseDTO> sendFriendRequest(@RequestParam Long fromUserId, @RequestParam Long toUserId) {
        log.info("Sending friend request from {} to {}", fromUserId, toUserId);

        Friend friendship = friendService.sendFriendRequest(fromUserId, toUserId);
        FriendResponseDTO response = FriendMapper.toFriendResponse(friendship);

        log.info("Friend request created successfully: {} -> {}", fromUserId, toUserId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{userId}")
    public ResponseEntity<FriendListResponseDTO> getUserFriends(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "50") int limit,
            @RequestParam(defaultValue = "name") String sort,
            @RequestParam(defaultValue = "all") String filter) {

        log.info("Getting friends for user {} with pagination: page={}, limit={}, sort={}, filter={}",
                userId, page, limit, sort, filter);

        List<User> friends = friendService.getFriends(userId);

        // Apply filtering
        List<User> filteredFriends = applyFriendFiltering(friends, filter);

        // Apply sorting
        List<User> sortedFriends = applyFriendSorting(filteredFriends, sort);

        // Apply pagination
        int totalCount = sortedFriends.size();
        int fromIndex = Math.max(0, (page - 1) * limit);
        int toIndex = Math.min(totalCount, fromIndex + limit);

        List<User> paginatedFriends = sortedFriends.subList(fromIndex, toIndex);

        // Convert to DTOs
        List<FriendResponseDTO> friendDTOs = FriendMapper.toFriendResponseListFromUsers(paginatedFriends, userId);
        int totalPages = (int) Math.ceil((double) totalCount / limit);

        FriendListResponseDTO response = FriendMapper.toFriendListResponse(
                friendDTOs, totalCount, page, limit, totalPages);

        log.info("Retrieved {} friends (page {}/{}) for user {}",
                paginatedFriends.size(), page, totalPages, userId);
        return ResponseEntity.ok(response);
    }

    private List<User> applyFriendFiltering(List<User> friends, String filter) {
        // For now, return all friends. Can be extended with availability filtering
        return friends;
    }

    private List<User> applyFriendSorting(List<User> friends, String sort) {
        switch (sort.toLowerCase()) {
            case "name":
                return friends.stream()
                        .sorted((u1, u2) -> u1.getName().compareToIgnoreCase(u2.getName()))
                        .toList();
            case "recent":
                // For now, maintain original order. Can be extended with friendship date
                return friends;
            case "availability":
                // For now, maintain original order. Can be extended with availability status
                return friends;
            default:
                return friends;
        }
    }

    @GetMapping("/{userId}/pending")
    public ResponseEntity<List<FriendResponseDTO>> getPendingRequests(@PathVariable Long userId) {
        log.info("Getting pending requests for user {}", userId);

        List<Friend> pendingRequests = friendService.getPendingRequests(userId);
        List<FriendResponseDTO> response = FriendMapper.toFriendResponseList(pendingRequests);

        log.info("Retrieved {} pending requests for user {}", pendingRequests.size(), userId);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{friendshipId}/accept")
    public ResponseEntity<FriendResponseDTO> acceptFriendRequest(@PathVariable Long friendshipId, @RequestParam Long userId) {
        log.info("Accepting friend request {} by user {}", friendshipId, userId);

        Friend updatedFriendship = friendService.acceptFriendRequest(friendshipId, userId);
        FriendResponseDTO response = FriendMapper.toFriendResponse(updatedFriendship);

        log.info("Friend request {} accepted successfully", friendshipId);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{friendshipId}/reject")
    public ResponseEntity<FriendResponseDTO> rejectFriendRequest(@PathVariable Long friendshipId, @RequestParam Long userId) {
        log.info("Rejecting friend request {} by user {}", friendshipId, userId);

        Friend updatedFriendship = friendService.rejectFriendRequest(friendshipId, userId);
        FriendResponseDTO response = FriendMapper.toFriendResponse(updatedFriendship);

        log.info("Friend request {} rejected successfully", friendshipId);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/remove")
    public ResponseEntity<SuccessResponseDTO> removeFriendship(@RequestParam Long userId1, @RequestParam Long userId2) {
        log.info("Removing friendship between {} and {}", userId1, userId2);

        boolean removed = friendService.removeFriendship(userId1, userId2);

        SuccessResponseDTO response = SuccessResponseDTO.of(
                removed ? "Friendship removed successfully" : "No friendship found to remove");

        log.info("Friendship removal between {} and {}: {}", userId1, userId2, removed ? "successful" : "not found");
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{userId}/all")
    public ResponseEntity<SuccessResponseDTO> removeAllFriendshipsForUser(@PathVariable Long userId) {
        log.info("Removing all friendships for user {}", userId);

        friendService.removeAllFriendshipsForUser(userId);

        SuccessResponseDTO response = SuccessResponseDTO.of("All friendships removed successfully");

        log.info("All friendships removed for user {}", userId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/check")
    public ResponseEntity<SuccessResponseDTO> checkFriendship(@RequestParam Long userId1, @RequestParam Long userId2) {
        log.debug("Checking friendship between {} and {}", userId1, userId2);

        boolean areFriends = friendService.areFriends(userId1, userId2);

        SuccessResponseDTO response = SuccessResponseDTO.builder()
                .message(areFriends ? "Users are friends" : "Users are not friends")
                .success(areFriends)
                .build();

        log.debug("Friendship check between {} and {}: {}", userId1, userId2, areFriends);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{userId}/stats")
    public ResponseEntity<FriendshipStatisticsDTO> getFriendshipStatistics(@PathVariable Long userId) {
        log.info("Getting friendship statistics for user {}", userId);

        Map<String, Long> stats = friendService.getFriendshipStatistics(userId);
        FriendshipStatisticsDTO response = FriendMapper.toFriendshipStatistics(stats);

        log.info("Retrieved friendship statistics for user {}: {}", userId, stats);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/mutual/{userId1}/{userId2}")
    public ResponseEntity<List<FriendResponseDTO>> getMutualFriends(@PathVariable Long userId1, @PathVariable Long userId2) {
        log.info("Getting mutual friends between users {} and {}", userId1, userId2);

        List<User> mutualFriends = friendService.getMutualFriends(userId1, userId2);
        List<FriendResponseDTO> response = FriendMapper.toFriendResponseListFromUsers(mutualFriends, userId1);

        log.info("Found {} mutual friends between users {} and {}", mutualFriends.size(), userId1, userId2);
        return ResponseEntity.ok(response);
    }
}