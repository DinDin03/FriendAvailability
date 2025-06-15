package com.friendavailability.service;

import com.friendavailability.model.Friend;
import com.friendavailability.model.User;
import org.springframework.stereotype.Service;
import java.util.*;

@Service //marks the class as a Spring service
public class FriendService {
    private final UserService userService; //stores the user service
    private List<Friend> friendships = new ArrayList<>(); //stores the friendships
    private Long nextId = 1L; //stores the next id

    public FriendService(UserService userService){ //constructor with user service
        this.userService = userService; //set the user service
        initialiseSampleData(); //initialize the sample data
        System.out.println("FriendService created with sample data"); //print the message
    }

    public void initialiseSampleData(){ //initialize the sample data
        Friend friendship1 = new Friend(1L, 2L, "ACCEPTED"); //create a friendship
        friendship1.setId(nextId++); //set the id
        friendships.add(friendship1); //add the friendship to the list

        // Alice (id=1) sent request to Charlie (we'll create user id=3) - pending
        Friend friendship2 = new Friend(1L, 3L, "PENDING"); //create a friendship
        friendship2.setId(nextId++); //set the id
        friendships.add(friendship2); //add the friendship to the list

        System.out.println("Sample friendships created");
    }
    public Friend sendFriendRequest(Long fromUserId, Long toUserId){ //send a friend request
        if(userService.findUserById(fromUserId).isEmpty()){ //check if the sender user is found
            throw new RuntimeException("Sender user not found with id: " + fromUserId); //throw an exception if the sender user is not found
        }
        if(userService.findUserById(toUserId).isEmpty()){ //check if the recipient user is found
            throw new RuntimeException("Recipient not found with id: " + toUserId); //throw an exception if the recipient user is not found
        }
        if(fromUserId.equals(toUserId)){ //check if the sender user is the same as the recipient user
            throw new RuntimeException("Can not send a request to yourself"); //throw an exception if the sender user is the same as the recipient user
        }
        if(friendshipExists(fromUserId, toUserId)){ //check if the friendship already exists
            throw new RuntimeException("You are already friends with " + toUserId); //throw an exception if the friendship already exists
        }

        Friend newFriendship = new Friend(fromUserId, toUserId); //create a new friendship
        newFriendship.setId(nextId++); //set the id
        friendships.add(newFriendship); //add the friendship to the list

        System.out.println("Friend request sent: " + newFriendship); //print the message
        return newFriendship;
    }

    private boolean friendshipExists(Long userId1, Long userId2) { //check if the friendship already exists
        return friendships.stream() //stream the friendships
                .anyMatch(f -> //check if the friendship exists
                        (f.getUserId().equals(userId1) && f.getFriendId().equals(userId2)) || //check if the friendship exists
                                (f.getUserId().equals(userId2) && f.getFriendId().equals(userId1)) //check if the friendship exists
                ); //return true if the friendship exists
    }

    public Friend acceptFriendRequests(Long friendshipId, Long personAcceptingId){ //accept a friend request
        Optional<Friend> friendshipOpt = friendships.stream() //stream the friendships
                .filter(f -> f.getId().equals(friendshipId)) //filter the friendships by id
                .findFirst(); //find the first friendship

        if(friendshipOpt.isEmpty()){ //check if the friendship is found
            throw new RuntimeException("Friend request not found with id: " + friendshipId); //throw an exception if the friendship is not found
        }

        Friend friendship = friendshipOpt.get(); //get the friendship

        if(!friendship.getFriendId().equals(personAcceptingId)){ //check if the person accepting the request is the recipient
            throw new RuntimeException("Only the reciepent can accept this request"); //throw an exception if the person accepting the request is not the recipient
        }

        if(!friendship.getStatus().equals("PENDING")){ //check if the request is pending
            throw new RuntimeException("Request is not pending"); //throw an exception if the request is not pending
        }

        friendship.setStatus("ACCEPTED"); //set the status to accepted
        System.out.println("Friend request accepted: " + friendship);
        return friendship;
    }

    public Friend rejectFriendRequests(Long friendshipId, Long personAcceptingId){ //reject a friend request
        Optional<Friend> friendshipOpt = friendships.stream() //stream the friendships
                .filter(f -> f.getId().equals(friendshipId)) //filter the friendships by id
                .findFirst(); //find the first friendship

        if(friendshipOpt.isEmpty()){ //check if the friendship is found
            throw new RuntimeException("Friend request not found with id: " + friendshipId); //throw an exception if the friendship is not found
        }

        Friend friendship = friendshipOpt.get(); //get the friendship

        if(!friendship.getFriendId().equals(personAcceptingId)) { //check if the person accepting the request is the recipient
            throw new RuntimeException("Only the reciepent can accept this request"); //throw an exception if the person accepting the request is not the recipient
        }

        friendship.setStatus("REJECTED"); //set the status to rejected
        System.out.println("Friend request rejected: " + friendship); //print the message
        return friendship;
    }

    public List<User> getFriends(Long userId){ //get the friends of a user
        List<Long> friendIds = friendships.stream() //stream the friendships
                .filter(f -> f.getStatus().equals("ACCEPTED")) //filter the friendships by status
                .filter(f -> f.getUserId().equals(userId) || f.getFriendId().equals(userId)) //filter the friendships by user id
                .map(f -> f.getUserId().equals(userId) ? f.getFriendId() : f.getUserId()) //map the friendships to the friend id
                .toList();

        return friendIds.stream() //stream the friend ids
                .map(userService::findUserById) //map the friend ids to the user
                .filter(Optional::isPresent) //filter the users by id
                .map(Optional::get) //get the user
                .toList();
    }

    public List<Friend> getPendingRequests(Long userId){ //get the pending requests of a user
        return friendships.stream() //stream the friendships
                .filter(f -> f.getFriendId().equals(userId)) //filter the friendships by user id
                .filter(f -> f.getStatus().equals("PENDING")) //filter the friendships by status
                .toList();
    }

    public boolean removeFriendship(Long userId1, Long userId2){ //remove a friendship
        boolean removed = friendships.removeIf(f -> //remove the friendship
                (f.getUserId().equals(userId1) && f.getFriendId().equals(userId2)) || //check if the friendship exists
                        (f.getUserId().equals(userId2) && f.getFriendId().equals(userId1))); //check if the friendship exists
        if(removed){ //check if the friendship is removed
            System.out.println("Friendship removed between users " + userId1 + " and " + userId2); //print the message
        }
        return removed; //return true if the friendship is removed
    }

    public void removeAllFriendshipsForUser(Long userId) { //remove all friendships for a user
        friendships.removeIf(f -> //remove the friendship
                f.getUserId().equals(userId) || f.getFriendId().equals(userId) //check if the friendship exists
        );
        System.out.println("All friendships removed for user " + userId); //print the message
    }

}
