package com.friendavailability.controller;

import com.friendavailability.model.Friend;
import com.friendavailability.model.User;
import com.friendavailability.service.FriendService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/friends")
public class FriendController {
    private final FriendService friendService;

    public FriendController(FriendService friendService) {
        this.friendService = friendService;
        System.out.println("FriendController created and connected to FriendService");
    }

    @PostMapping("/request")
    public ResponseEntity<Friend> sendFriendRequest(@RequestParam Long fromUserId, @RequestParam Long toUserId) {
        System.out.println("Sending friend request from " + fromUserId + " to " + toUserId);

        try {
            Friend friendship = friendService.sendFriendRequest(fromUserId, toUserId);
            return ResponseEntity.status(HttpStatus.CREATED).body(friendship);
        } catch (RuntimeException e) {
            System.err.println("Business logic error sending friend request: " + e.getMessage());

            if (e.getMessage().contains("not found")) {
                return ResponseEntity.notFound().build();
            } else if (e.getMessage().contains("yourself") || e.getMessage().contains("already exists")) {
                return ResponseEntity.badRequest().build();
            } else {
                return ResponseEntity.badRequest().build();
            }
        } catch (Exception e) {
            System.err.println("Unexpected error sending friend request: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/{userId}")
    public ResponseEntity<List<User>> getUserFriends(@PathVariable Long userId) {
        System.out.println("GET /api/friends/" + userId + " - Getting friends for user " + userId);

        try {
            List<User> friends = friendService.getFriends(userId);
            return ResponseEntity.ok(friends);
        } catch (RuntimeException e) {
            System.err.println("Business logic error getting friends: " + e.getMessage());
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            System.err.println("Unexpected error getting friends for user " + userId + ": " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/{userId}/pending")
    public ResponseEntity<List<Friend>> getPendingRequests(@PathVariable Long userId) {
        System.out.println("Getting pending requests for user " + userId);

        try {
            List<Friend> pendingRequests = friendService.getPendingRequests(userId);
            return ResponseEntity.ok(pendingRequests);
        } catch (RuntimeException e) {
            System.err.println("Business logic error getting pending requests: " + e.getMessage());
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            System.err.println("Unexpected error getting pending requests for user " + userId + ": " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PutMapping("/{friendshipId}/accept")
    public ResponseEntity<Friend> acceptFriendRequest(@PathVariable Long friendshipId, @RequestParam Long userId) {
        System.out.println("Accepting friend request " + friendshipId + " by user " + userId);

        try {
            Friend updatedFriendship = friendService.acceptFriendRequest(friendshipId, userId);
            return ResponseEntity.ok(updatedFriendship);
        } catch (RuntimeException e) {
            System.err.println("Business logic error accepting friend request: " + e.getMessage());

            if (e.getMessage().contains("not found")) {
                return ResponseEntity.notFound().build();
            } else if (e.getMessage().contains("Only the recipient") || e.getMessage().contains("not pending")) {
                return ResponseEntity.badRequest().build();
            } else {
                return ResponseEntity.badRequest().build();
            }
        } catch (Exception e) {
            System.err.println("Unexpected error accepting friend request: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PutMapping("/{friendshipId}/reject")
    public ResponseEntity<Friend> rejectFriendRequest(@PathVariable Long friendshipId, @RequestParam Long userId) {
        System.out.println("Rejecting friend request " + friendshipId + " by user " + userId);

        try {
            Friend updatedFriendship = friendService.rejectFriendRequest(friendshipId, userId);
            return ResponseEntity.ok(updatedFriendship);
        } catch (RuntimeException e) {
            System.err.println("Business logic error rejecting friend request: " + e.getMessage());

            if (e.getMessage().contains("not found")) {
                return ResponseEntity.notFound().build();
            } else if (e.getMessage().contains("Only the recipient")) {
                return ResponseEntity.badRequest().build();
            } else {
                return ResponseEntity.badRequest().build();
            }
        } catch (Exception e) {
            System.err.println("Unexpected error rejecting friend request: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @DeleteMapping("/remove")
    public ResponseEntity<String> removeFriendship(@RequestParam Long userId1, @RequestParam Long userId2) {
        System.out.println("Removing friendship between " + userId1 + " and " + userId2);

        try {
            boolean removed = friendService.removeFriendship(userId1, userId2);

            if (removed) {
                String successMessage = "Friendship removed successfully between users " + userId1 + " and " + userId2;
                return ResponseEntity.ok(successMessage);
            } else {
                String notFoundMessage = "No friendship found between users " + userId1 + " and " + userId2;
                return ResponseEntity.notFound().build();
            }
        } catch (RuntimeException e) {
            System.err.println("Business logic error removing friendship: " + e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            System.err.println("Unexpected error removing friendship between " + userId1 + " and " + userId2 + ": " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @DeleteMapping("/{userId}/all")
    public ResponseEntity<String> removeAllFriendshipsForUser(@PathVariable Long userId) {
        System.out.println("Removing all friendships for user " + userId);

        try {
            friendService.removeAllFriendshipsForUser(userId);
            String successMessage = "All friendships for user " + userId + " have been removed";
            return ResponseEntity.ok(successMessage);
        } catch (RuntimeException e) {
            System.err.println("Business logic error removing all friendships: " + e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            System.err.println("Unexpected error removing all friendships for user " + userId + ": " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/check")
    public ResponseEntity<Map<String, Boolean>> checkFriendship(@RequestParam Long userId1, @RequestParam Long userId2) {
        System.out.println("Checking friendship between " + userId1 + " and " + userId2);

        try {
            boolean areFriends = friendService.areFriends(userId1, userId2);
            Map<String, Boolean> response = Map.of("areFriends", areFriends);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            System.err.println("Error checking friendship between " + userId1 + " and " + userId2 + ": " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/{userId}/stats")
    public ResponseEntity<Map<String, Long>> getFriendshipStatistics(@PathVariable Long userId) {
        System.out.println("Getting friendship statistics for user " + userId);

        try {
            Map<String, Long> stats = friendService.getFriendshipStatistics(userId);
            return ResponseEntity.ok(stats);
        } catch (RuntimeException e) {
            System.err.println("Business logic error getting friendship statistics: " + e.getMessage());
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            System.err.println("Unexpected error getting friendship statistics for user " + userId + ": " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}