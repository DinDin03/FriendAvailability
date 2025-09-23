package com.friendavailability.domain.service;

import com.friendavailability.domain.entity.Friend;
import com.friendavailability.domain.entity.User;
import com.friendavailability.domain.exception.InvalidOperationException;
import com.friendavailability.domain.exception.InsufficientPermissionException;
import com.friendavailability.domain.exception.ResourceNotFoundException;
import com.friendavailability.domain.repository.FriendRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

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
            log.warn("User {} attempted to send a friend request to themselves", fromUserId);
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
        log.debug("Accepting friend request {} by user {}", friendshipId, personAcceptingId);

        Friend friendship = findFriendRequestById(friendshipId);

        if (!friendship.getFriendId().equals(personAcceptingId)) {
            log.warn("User {} attempted to accept friend request {} but is not the recipient", personAcceptingId, friendshipId);
            throw InsufficientPermissionException.onlyRecipientsCanRespond("accept");
        }

        if (!friendship.getStatus().equals("PENDING")) {
            log.warn("Attempted to accept friend request {} with status {}", friendshipId, friendship.getStatus());
            throw InvalidOperationException.friendRequestNotPending(friendship.getStatus());
        }

        friendship.setStatus("ACCEPTED");
        Friend updatedFriendship = friendRepository.save(friendship);
        log.info("Friend request {} accepted by user {}", friendshipId, personAcceptingId);
        return updatedFriendship;
    }

    public Friend rejectFriendRequest(Long friendshipId, Long personRejectingId) {
        log.debug("Rejecting friend request {} by user {}", friendshipId, personRejectingId);

        Friend friendship = findFriendRequestById(friendshipId);

        if (!friendship.getFriendId().equals(personRejectingId)) {
            log.warn("User {} attempted to reject friend request {} but is not the recipient", personRejectingId, friendshipId);
            throw InsufficientPermissionException.onlyRecipientsCanRespond("reject");
        }

        if (!friendship.getStatus().equals("PENDING")) {
            log.warn("Attempted to reject friend request {} with status {}", friendshipId, friendship.getStatus());
            throw InvalidOperationException.friendRequestNotPending(friendship.getStatus());
        }

        friendship.setStatus("REJECTED");
        Friend updatedFriendship = friendRepository.save(friendship);
        log.info("Friend request {} rejected by user {}", friendshipId, personRejectingId);
        return updatedFriendship;
    }

    public List<User> getFriends(Long userId) {
        log.debug("Getting all friends for user {}", userId);

        userService.findUserById(userId);

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
                .toList();

        log.debug("Found {} friends for user {}", friends.size(), userId);
        return friends;
    }

    public List<Friend> getPendingRequests(Long userId) {
        log.debug("Getting all pending requests for user {}", userId);

        userService.findUserById(userId);

        List<Friend> pendingRequests = friendRepository.findByFriendIdAndStatus(userId, "PENDING");

        log.debug("Found {} pending requests for user {}", pendingRequests.size(), userId);
        return pendingRequests;
    }

    public boolean removeFriendship(Long userId1, Long userId2) {
        log.debug("Removing friendship between users {} and {}", userId1, userId2);

        userService.findUserById(userId1);
        userService.findUserById(userId2);

        boolean exists = friendRepository.existsFriendshipBetweenUsers(userId1, userId2);

        if (exists) {
            friendRepository.deleteFriendshipBetweenUsers(userId1, userId2);
            log.info("Friendship removed between users {} and {}", userId1, userId2);
            return true;
        } else {
            log.debug("No friendship found between users {} and {}", userId1, userId2);
            return false;
        }
    }

    public void removeAllFriendshipsForUser(Long userId) {
        log.debug("Removing all friendships for user {}", userId);

        userService.findUserById(userId);

        long friendshipCount = friendRepository.findAllFriendshipsForUser(userId).size();
        friendRepository.deleteAllFriendshipsForUser(userId);
        log.info("Removed {} friendships for user {}", friendshipCount, userId);
    }

    public boolean areFriends(Long userId1, Long userId2) {
        log.debug("Checking friendship between users {} and {}", userId1, userId2);

        userService.findUserById(userId1);
        userService.findUserById(userId2);

        Optional<Friend> friendship = friendRepository.findFriendshipBetweenUsers(userId1, userId2);
        boolean areFriends = friendship.isPresent() && "ACCEPTED".equals(friendship.get().getStatus());

        log.debug("Users {} and {} are friends: {}", userId1, userId2, areFriends);
        return areFriends;
    }

    public Map<String, Long> getFriendshipStatistics(Long userId) {
        log.debug("Getting friendship statistics for user {}", userId);

        userService.findUserById(userId);

        Map<String, Long> stats = new HashMap<>();
        stats.put("totalFriends", friendRepository.countFriendsForUser(userId));
        stats.put("pendingRequests", friendRepository.countByFriendIdAndStatus(userId, "PENDING"));

        log.debug("Friendship statistics for user {}: {}", userId, stats);
        return stats;
    }

    private Friend findFriendRequestById(Long friendshipId) {
        return friendRepository.findById(friendshipId)
                .orElseThrow(() -> {
                    log.warn("Friend request not found with id {}", friendshipId);
                    return ResourceNotFoundException.friendRequestNotFound(friendshipId);
                });
    }
}