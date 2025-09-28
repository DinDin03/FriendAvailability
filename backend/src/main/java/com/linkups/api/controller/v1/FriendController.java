package com.linkups.api.controller.v1;

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
    public ResponseEntity<Friend> sendFriendRequest(@RequestParam Long fromUserId, @RequestParam Long toUserId) {
        log.info("Sending friend request from {} to {}", fromUserId, toUserId);

        Friend friendship = friendService.sendFriendRequest(fromUserId, toUserId);

        log.info("Friend request created successfully: {} -> {}", fromUserId, toUserId);
        return ResponseEntity.status(HttpStatus.CREATED).body(friendship);
    }

    @GetMapping("/{userId}")
    public ResponseEntity<Object> getUserFriends(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "50") int limit,
            @RequestParam(defaultValue = "name") String sort,
            @RequestParam(defaultValue = "all") String filter) {

        log.info("Getting friends for user {} with pagination: page={}, limit={}, sort={}, filter={}",
                userId, page, limit, sort, filter);

        // If pagination is requested (page > 1 or limit != default large value)
        if (page > 1 || limit < 1000) {
            // Return paginated response
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

            Map<String, Object> paginatedResponse = Map.of(
                "friends", paginatedFriends,
                "totalCount", totalCount,
                "page", page,
                "limit", limit,
                "totalPages", (int) Math.ceil((double) totalCount / limit)
            );

            log.info("Retrieved {} friends (page {}/{}) for user {}",
                    paginatedFriends.size(), page, (int) Math.ceil((double) totalCount / limit), userId);
            return ResponseEntity.ok(paginatedResponse);
        } else {
            // Return simple list for backward compatibility
            List<User> friends = friendService.getFriends(userId);
            log.info("Retrieved {} friends for user {}", friends.size(), userId);
            return ResponseEntity.ok(friends);
        }
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
    public ResponseEntity<List<Friend>> getPendingRequests(@PathVariable Long userId) {
        log.info("Getting pending requests for user {}", userId);

        List<Friend> pendingRequests = friendService.getPendingRequests(userId);

        log.info("Retrieved {} pending requests for user {}", pendingRequests.size(), userId);
        return ResponseEntity.ok(pendingRequests);
    }

    @PutMapping("/{friendshipId}/accept")
    public ResponseEntity<Friend> acceptFriendRequest(@PathVariable Long friendshipId, @RequestParam Long userId) {
        log.info("Accepting friend request {} by user {}", friendshipId, userId);

        Friend updatedFriendship = friendService.acceptFriendRequest(friendshipId, userId);

        log.info("Friend request {} accepted successfully", friendshipId);
        return ResponseEntity.ok(updatedFriendship);
    }

    @PutMapping("/{friendshipId}/reject")
    public ResponseEntity<Friend> rejectFriendRequest(@PathVariable Long friendshipId, @RequestParam Long userId) {
        log.info("Rejecting friend request {} by user {}", friendshipId, userId);

        Friend updatedFriendship = friendService.rejectFriendRequest(friendshipId, userId);

        log.info("Friend request {} rejected successfully", friendshipId);
        return ResponseEntity.ok(updatedFriendship);
    }

    @DeleteMapping("/remove")
    public ResponseEntity<Map<String, Object>> removeFriendship(@RequestParam Long userId1, @RequestParam Long userId2) {
        log.info("Removing friendship between {} and {}", userId1, userId2);

        boolean removed = friendService.removeFriendship(userId1, userId2);

        Map<String, Object> response = Map.of(
                "success", removed,
                "message", removed ? "Friendship removed successfully" : "No friendship found to remove"
        );

        log.info("Friendship removal between {} and {}: {}", userId1, userId2, removed ? "successful" : "not found");
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{userId}/all")
    public ResponseEntity<Map<String, String>> removeAllFriendshipsForUser(@PathVariable Long userId) {
        log.info("Removing all friendships for user {}", userId);

        friendService.removeAllFriendshipsForUser(userId);

        Map<String, String> response = Map.of("message", "All friendships removed successfully");

        log.info("All friendships removed for user {}", userId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/check")
    public ResponseEntity<Map<String, Boolean>> checkFriendship(@RequestParam Long userId1, @RequestParam Long userId2) {
        log.debug("Checking friendship between {} and {}", userId1, userId2);

        boolean areFriends = friendService.areFriends(userId1, userId2);

        Map<String, Boolean> response = Map.of("areFriends", areFriends);

        log.debug("Friendship check between {} and {}: {}", userId1, userId2, areFriends);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{userId}/stats")
    public ResponseEntity<Map<String, Long>> getFriendshipStatistics(@PathVariable Long userId) {
        log.info("Getting friendship statistics for user {}", userId);

        Map<String, Long> stats = friendService.getFriendshipStatistics(userId);

        log.info("Retrieved friendship statistics for user {}: {}", userId, stats);
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/mutual/{userId1}/{userId2}")
    public ResponseEntity<List<User>> getMutualFriends(@PathVariable Long userId1, @PathVariable Long userId2) {
        log.info("Getting mutual friends between users {} and {}", userId1, userId2);

        List<User> mutualFriends = friendService.getMutualFriends(userId1, userId2);

        log.info("Found {} mutual friends between users {} and {}", mutualFriends.size(), userId1, userId2);
        return ResponseEntity.ok(mutualFriends);
    }
}