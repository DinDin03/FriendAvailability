package com.friendavailability.controller;

import com.friendavailability.model.Friend;
import com.friendavailability.model.User;
import com.friendavailability.service.FriendService;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController //marks the class as a Spring REST controller
@RequestMapping("/api/friends") //maps the class to the /api/friends endpoint
public class FriendController { //defines the friend controller
    private final FriendService friendService; //stores the friend service

    public FriendController(FriendService friendService){ //constructor with friend service
        this.friendService = friendService; //set the friend service
        System.out.println("FriendController created and connected to FriendService"); //print the message
    }

    @PostMapping("/request")
    public Friend sendFriendRequest(@RequestParam Long fromUserId, @RequestParam Long toUserId){ //send a friend request
        System.out.println("Request sent from " + fromUserId + " to " + toUserId); //print the message
        return friendService.sendFriendRequest(fromUserId, toUserId); //send a friend request
    }

    @GetMapping("/{userId}") //get all friends from a user
    public List<User> getUserFriends(@PathVariable Long userId){ //get all friends from a user
        System.out.println("Get all friends from user " + userId); //print the message
        return friendService.getFriends(userId); //get all friends from a user
    }

    @GetMapping("/{userId}/pending") //get all pending requests from a user
    public List<Friend> getPendingRequests(@PathVariable Long userId){ //get all pending requests from a user
        System.out.println("Get pending requests for user with id " + userId); //print the message
        return friendService.getPendingRequests(userId); //get all pending requests from a user
    }

    @PutMapping("/{friendshipId}/accept") //accept a friend request
    public Friend acceptFriendRequest(@PathVariable Long friendshipId, @RequestParam Long userId){ //accept a friend request
        System.out.println("Accepting friend request with id " + friendshipId); //print the message
        return friendService.acceptFriendRequests(friendshipId, userId); //accept a friend request
    }

    @PutMapping("/{friendshipId}/reject")
    public Friend rejectFriendRequest(@PathVariable Long friendshipId, @RequestParam Long userId){
        System.out.println("Rejecting friend request with id " + friendshipId);
        return friendService.rejectFriendRequests(friendshipId, userId);
    }

    @DeleteMapping("/remove") //remove a friendship
    public String removeFriendship(@RequestParam Long userId1, @RequestParam Long userId2) { //remove a friendship
        System.out.println("Removing friendship between " + userId1 + " and " + userId2); //print the message
        boolean removed = friendService.removeFriendship(userId1, userId2); //remove a friendship

        if (removed) { //check if the friendship is removed
            return "Friendship removed successfully between users " + userId1 + " and " + userId2; //return the message
        } else { //check if the friendship is not removed
            return "No friendship found between users " + userId1 + " and " + userId2; //return the message
        }
    }

    @DeleteMapping("/{userId}/all") //remove all friendships for a user
    public String removeAllFriendshipsForUser(@PathVariable Long userId) { //remove all friendships for a user
        System.out.println("Removing all friendships for user " + userId); //print the message
        friendService.removeAllFriendshipsForUser(userId); //remove all friendships for a user
        return ("All friendships for user " + userId + " have been removed."); //return the message
    }

}
