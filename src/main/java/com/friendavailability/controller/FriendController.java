package com.friendavailability.controller;

import com.friendavailability.model.Friend;
import com.friendavailability.model.User;
import com.friendavailability.service.FriendService;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/friends")
public class FriendController {
    private final FriendService friendService;

    public FriendController(FriendService friendService){
        this.friendService = friendService;
        System.out.println("FriendController created and connected to FriendService");
    }

    @PostMapping("/request")
    public Friend sendFriendRequest(@RequestParam Long fromUserId, @RequestParam Long toUserId){
        System.out.println("Request sent from " + fromUserId + " to " + toUserId);
        return friendService.sendFriendRequest(fromUserId, toUserId);
    }

    @GetMapping("/{userId}")
    public List<User> getUserFriends(@PathVariable Long userId){
        System.out.println("Get all friends from user " + userId);
        return friendService.getFriends(userId);
    }

    @GetMapping("/{userId}/pending")
    public List<Friend> getPendingRequests(@PathVariable Long userId){
        System.out.println("Get pending requests for user with id " + userId);
        return friendService.getPendingRequests(userId);
    }

    @PutMapping("/{friendshipId}/accept")
    public Friend acceptFriendRequest(@PathVariable Long friendshipId, @RequestParam Long userId){
        System.out.println("Accepting friend request with id " + friendshipId);
        return friendService.acceptFriendRequests(friendshipId, userId);
    }

    @PutMapping("/{friendshipId}/reject")
    public Friend rejectFriendRequest(@PathVariable Long friendshipId, @RequestParam Long userId){
        System.out.println("Rejecting friend request with id " + friendshipId);
        return friendService.rejectFriendRequests(friendshipId, userId);
    }

    @DeleteMapping("/remove")
    public String removeFriendship(@RequestParam Long userId1, @RequestParam Long userId2) {
        System.out.println("Removing friendship between " + userId1 + " and " + userId2);
        boolean removed = friendService.removeFriendship(userId1, userId2);

        if (removed) {
            return "Friendship removed successfully between users " + userId1 + " and " + userId2;
        } else {
            return "No friendship found between users " + userId1 + " and " + userId2;
        }
    }
}
