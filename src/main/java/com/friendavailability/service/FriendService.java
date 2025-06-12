package com.friendavailability.service;

import com.friendavailability.model.Friend;
import com.friendavailability.model.User;
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class FriendService {
    private final UserService userService;
    private List<Friend> friendships = new ArrayList<>();
    private Long nextId = 1L;

    public FriendService(UserService userService){
        this.userService = userService;
        initialiseSampleData();
        System.out.println("FriendService created with sample data");
    }

    public void initialiseSampleData(){
        Friend friendship1 = new Friend(1L, 2L, "ACCEPTED");
        friendship1.setId(nextId++);
        friendships.add(friendship1);

        // Alice (id=1) sent request to Charlie (we'll create user id=3) - pending
        Friend friendship2 = new Friend(1L, 3L, "PENDING");
        friendship2.setId(nextId++);
        friendships.add(friendship2);

        System.out.println("Sample friendships created");
    }
    public Friend sendFriendRequest(Long fromUserId, Long toUserId){
        if(userService.findUserById(fromUserId).isEmpty()){
            throw new RuntimeException("Sender user not found with id: " + fromUserId);
        }
        if(userService.findUserById(toUserId).isEmpty()){
            throw new RuntimeException("Recipient not found with id: " + toUserId);
        }
        if(fromUserId.equals(toUserId)){
            throw new RuntimeException("Can not send a request to yourself");
        }
        if(friendshipExists(fromUserId, toUserId)){
            throw new RuntimeException("You are already friends with " + toUserId);
        }

        Friend newFriendship = new Friend(fromUserId, toUserId);
        newFriendship.setId(nextId++);
        friendships.add(newFriendship);

        System.out.println("Friend request sent: " + newFriendship);
        return newFriendship;
    }

    private boolean friendshipExists(Long userId1, Long userId2) {
        return friendships.stream()
                .anyMatch(f ->
                        (f.getUserId().equals(userId1) && f.getFriendId().equals(userId2)) ||
                                (f.getUserId().equals(userId2) && f.getFriendId().equals(userId1))
                );
    }

    public Friend acceptFriendRequests(Long friendshipId, Long personAcceptingId){
        Optional<Friend> friendshipOpt = friendships.stream()
                .filter(f -> f.getId().equals(friendshipId))
                .findFirst();

        if(friendshipOpt.isEmpty()){
            throw new RuntimeException("Friend request not found with id: " + friendshipId);
        }

        Friend friendship = friendshipOpt.get();

        if(!friendship.getFriendId().equals(personAcceptingId)){
            throw new RuntimeException("Only the reciepent can accept this request");
        }

        if(!friendship.getStatus().equals("PENDING")){
            throw new RuntimeException("Request is not pending");
        }

        friendship.setStatus("ACCEPTED");
        System.out.println("Friend request accepted: " + friendship);
        return friendship;
    }

    public Friend rejectFriendRequests(Long friendshipId, Long personAcceptingId){
        Optional<Friend> friendshipOpt = friendships.stream()
                .filter(f -> f.getId().equals(friendshipId))
                .findFirst();

        if(friendshipOpt.isEmpty()){
            throw new RuntimeException("Friend request not found with id: " + friendshipId);
        }

        Friend friendship = friendshipOpt.get();

        if(!friendship.getFriendId().equals(personAcceptingId)) {
            throw new RuntimeException("Only the reciepent can accept this request");
        }

        friendship.setStatus("REJECTED");
        System.out.println("Friend request rejected: " + friendship);
        return friendship;
    }

    public List<User> getFriends(Long userId){
        List<Long> friendIds = friendships.stream()
                .filter(f -> f.getStatus().equals("ACCEPTED"))
                .filter(f -> f.getUserId().equals(userId) || f.getFriendId().equals(userId))
                .map(f -> f.getUserId().equals(userId) ? f.getFriendId() : f.getUserId())
                .toList();

        return friendIds.stream()
                .map(userService::findUserById)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .toList();
    }

    public List<Friend> getPendingRequests(Long userId){
        return friendships.stream()
                .filter(f -> f.getFriendId().equals(userId))
                .filter(f -> f.getStatus().equals("PENDING"))
                .toList();
    }

    public boolean removeFriendship(Long userId1, Long userId2){
        boolean removed = friendships.removeIf(f ->
                (f.getUserId().equals(userId1) && f.getFriendId().equals(userId2)) ||
                        (f.getUserId().equals(userId2) && f.getFriendId().equals(userId1)));
        if(removed){
            System.out.println("Friendship removed between users " + userId1 + " and " + userId2);
        }
        return removed;
    }

    public void removeAllFriendshipsForUser(Long userId) {
        friendships.removeIf(f ->
                f.getUserId().equals(userId) || f.getFriendId().equals(userId)
        );
        System.out.println("All friendships removed for user " + userId);
    }

}
