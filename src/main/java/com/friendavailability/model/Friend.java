package com.friendavailability.model;

import java.time.LocalDateTime;

public class Friend { //defines the friend entity
    private Long id; //stores the internal id of the friend
    private Long userId; //stores the id of the user
    private Long friendId; //stores the id of the friend
    private String status; //stores the status of the friend
    private LocalDateTime createdAt; //stores the creation time of the friend
    private LocalDateTime updatedAt; //stores the last update time of the friend

    public Friend(){ //default constructor
        this.createdAt = LocalDateTime.now(); //set the creation time
        this.updatedAt = LocalDateTime.now(); //set the last update time
    }

    public Friend(Long userId, Long friendId){ //constructor with user id and friend id
        this.userId = userId; //set the user id
        this.friendId = friendId; //set the friend id
        this.status = "PENDING"; //set the status to pending
        this.createdAt = LocalDateTime.now(); //set the creation time
        this.updatedAt = LocalDateTime.now(); //set the last update time
    }

    public Friend(Long userId, Long friendId, String status) { //constructor with user id, friend id and status
        this.userId = userId; //set the user id
        this.friendId = friendId; //set the friend id
        this.status = status; //set the status
        this.createdAt = LocalDateTime.now(); //set the creation time
        this.updatedAt = LocalDateTime.now(); //set the last update time
    }

    public Long getId() { //get the id
        return id; //return the id
    }

    public void setId(Long id) { //set the id
        this.id = id; //set the id
    }

    public Long getUserId() { //get the user id
        return userId; //return the user id
    }

    public void setUserId(Long userId) { //set the user id
        this.userId = userId; //set the user id
    }

    public Long getFriendId() { //get the friend id
        return friendId; //return the friend id
    }

    public void setFriendId(Long friendId) { //set the friend id
        this.friendId = friendId; //set the friend id
    }

    public String getStatus() { //get the status
        return status; //return the status
    }

    public void setStatus(String status) { //set the status
        this.status = status; //set the status
        this.updatedAt = LocalDateTime.now(); //set the last update time
    }

    public LocalDateTime getCreatedAt() { //get the creation time
        return createdAt; //return the creation time
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() { //get the last update time
        return updatedAt; //return the last update time
    }

    public void setUpdatedAt(LocalDateTime updatedAt) { //set the last update time
        this.updatedAt = updatedAt; //set the last update time
    }

    @Override
    public String toString() { //return the string representation of the friend
        return "Friend{" + //return the string representation of the friend
                "id=" + id + //return the id
                ", userId=" + userId + //return the user id
                ", friendId=" + friendId + //return the friend id
                ", status='" + status + '\'' + //return the status
                ", createdAt=" + createdAt + //return the creation time
                ", updatedAt=" + updatedAt + //return the last update time
                '}'; //return the string representation of the friend
    }
}
