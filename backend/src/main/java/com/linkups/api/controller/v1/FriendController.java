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
    public ResponseEntity<List<User>> getUserFriends(@PathVariable Long userId) {
        log.info("Getting friends for user {}", userId);

        List<User> friends = friendService.getFriends(userId);

        log.info("Retrieved {} friends for user {}", friends.size(), userId);
        return ResponseEntity.ok(friends);
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
}