package com.friendavailability.domain.service;

import com.friendavailability.domain.entity.Friend;
import com.friendavailability.domain.entity.User;
import com.friendavailability.domain.exception.InvalidOperationException;
import com.friendavailability.domain.exception.ResourceNotFoundException;
import com.friendavailability.domain.repository.FriendRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
@Slf4j
public class FriendService {

    private final UserService userService;
    private final FriendRepository friendRepository;

    public FriendService(UserService userService, FriendRepository friendRepository) {
        this.userService = userService;
        this.friendRepository = friendRepository;
        log.info("Friend service created");
    }

    public Friend sendFriendRequest(Long fromUserId, Long toUserId) {
        log.debug("Sending friend request from user {} to user {}", fromUserId, toUserId);

        User fromUser = userService.findUserById(fromUserId);
        User toUser = userService.findUserById(toUserId);

        if (fromUserId.equals(toUserId)) {
            log.warn("User {} attempted to send a friend requets to themselves", fromUserId);
            throw InvalidOperationException.cannotAddSelfAsFriend(fromUserId);
        }

        if (friendRepository.existsFriendshipBetweenUsers(fromUserId, toUserId)) {
            log.warn("Friendship between users {} and {} already exists", fromUserId, toUserId);
            throw InvalidOperationException.friendShipAlreadyExists(fromUserId, toUserId);
        }
        Friend newFriendship = Friend.builder()
                .userId(fromUserId)
                .friendId(toUserId)
                .status("PENDING")
                .build();

        Friend savedFriendship = friendRepository.save(newFriendship);
        log.info("Friend request created from user {} to user {}", fromUserId, toUserId);
        return savedFriendship;
    }

    public Friend acceptFriendRequest(Long friendshipId, Long personAcceptingId) {
        System.out.println("Accepting friend request with id " + friendshipId + " by user " + personAcceptingId);

        Optional<Friend> friendshipOpt = friendRepository.findById(friendshipId);
        if (friendshipOpt.isEmpty()) {
            throw new RuntimeException("Friend request not found with id " + friendshipId);
        }
        Friend friendship = friendshipOpt.get();

        if (!friendship.getFriendId().equals(personAcceptingId)) {
            throw new RuntimeException("Only the recipient can accept this request");
        }
        if (!friendship.getStatus().equals("PENDING")) {
            throw new RuntimeException("Friend request is not pending (Current status: " + friendship.getStatus() + ")");
        }
        friendship.setStatus("ACCEPTED");
        Friend updatedFriendship = friendRepository.save(friendship);
        System.out.println("Friend request accepted: " + updatedFriendship);
        return updatedFriendship;
    }

    public Friend rejectFriendRequest(Long friendshipId, Long personRejectingId) {
        System.out.println("Rejecting friend request with id " + friendshipId + " by user " + personRejectingId);

        Optional<Friend> friendshipOpt = friendRepository.findById(friendshipId);
        if (friendshipOpt.isEmpty()) {
            throw new RuntimeException("Friend request not found with id " + friendshipId);
        }
        Friend friendship = friendshipOpt.get();

        if (!friendship.getFriendId().equals(personRejectingId)) {
            throw new RuntimeException("Only the recipient can reject this request");
        }

        friendship.setStatus("REJECTED");
        Friend updatedFriendship = friendRepository.save(friendship);
        System.out.println("Friend request rejected: " + updatedFriendship);
        return updatedFriendship;
    }

    public List<User> getFriends(Long userId) {
        System.out.println("Getting all friends for user " + userId);

        List<Friend> acceptedFriendships = friendRepository.findAcceptedFriendshipsForUser(userId);

        List<Long> friendIds = acceptedFriendships.stream()
                .map(friendship -> {
                    if (friendship.getUserId().equals(userId)) {
                        return friendship.getFriendId();
                    } else {
                        return friendship.getUserId();
                    }
                })
                .toList();

        List<User> friends = friendIds.stream()
                .map(userService::findUserById)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .toList();

        System.out.println("Found " + friends.size() + " friends for user " + userId);
        return friends;
    }

    public List<Friend> getPendingRequests(Long userId){
        System.out.println("Getting all pending requests for user with id " + userId);

        List<Friend> pendingRequests = friendRepository.findByFriendIdAndStatus(userId, "PENDING");

        System.out.println("Found " + pendingRequests.size() + " pending requests for user " + userId);
        return pendingRequests;
    }

    public boolean removeFriendship(Long userId1, Long userId2){
        System.out.println("Removing friendship between users " + userId1 + " and " + userId2);

        boolean exists = friendRepository.existsFriendshipBetweenUsers(userId1, userId2);

        if(exists){
            friendRepository.deleteFriendshipBetweenUsers(userId1, userId2);
            System.out.println("Friendship removed between users " + userId1 + " and " + userId2);
            return true;
        }else {
            System.out.println("No friendship found between users " + userId1 + " and " + userId2);
            return false;
        }
    }

    public void removeAllFriendshipsForUser(Long userId){
        System.out.println("Removing all friendships for user with id " + userId);
        long friendshipCount = friendRepository.findAllFriendshipsForUser(userId).size();
        friendRepository.deleteAllFriendshipsForUser(userId);
        System.out.println("Removed " + friendshipCount + " friendships for user " + userId);
    }

    public boolean areFriends(Long userId1, Long userId2) {
        Optional<Friend> friendship = friendRepository.findFriendshipBetweenUsers(userId1, userId2);
        return friendship.isPresent() && "ACCEPTED".equals(friendship.get().getStatus());
    }

    public Map<String, Long> getFriendshipStatistics(Long userId) {
        Map<String, Long> stats = new HashMap<>();

        stats.put("totalFriends", friendRepository.countFriendsForUser(userId));
        stats.put("pendingRequests", friendRepository.countByFriendIdAndStatus(userId, "PENDING"));

        return stats;
    }
}