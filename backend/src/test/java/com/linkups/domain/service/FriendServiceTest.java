package com.linkups.domain.service;

import com.linkups.base.BaseUnitTest;
import com.linkups.domain.entity.Friend;
import com.linkups.domain.entity.User;
import com.linkups.domain.exception.InvalidOperationException;
import com.linkups.domain.exception.InsufficientPermissionException;
import com.linkups.domain.repository.FriendRepository;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.*;

//all tests passed
class FriendServiceTest extends BaseUnitTest {

    @Mock
    private UserService userService;
    
    @Mock
    private FriendRepository friendRepository;

    @InjectMocks
    private FriendService friendService;

    @Test
    void shouldSendFriendRequestSuccessfully() {
        // Given
        Long fromUserId = 1L;
        Long toUserId = 2L;
        
        User fromUser = User.builder().id(fromUserId).email("from@example.com").name("From User").build();
        User toUser = User.builder().id(toUserId).email("to@example.com").name("To User").build();
        
        Friend savedFriendship = Friend.builder()
                .id(10L)
                .userId(fromUserId)
                .friendId(toUserId)
                .status("PENDING")
                .build();
        
        given(userService.findUserById(fromUserId)).willReturn(fromUser);
        given(userService.findUserById(toUserId)).willReturn(toUser);
        given(friendRepository.existsFriendshipBetweenUsers(fromUserId, toUserId)).willReturn(false);
        given(friendRepository.save(any(Friend.class))).willReturn(savedFriendship);

        // When
        Friend result = friendService.sendFriendRequest(fromUserId, toUserId);

        // Then
        assertThat(result).isEqualTo(savedFriendship);
        assertThat(result.getStatus()).isEqualTo("PENDING");
        assertThat(result.getUserId()).isEqualTo(fromUserId);
        assertThat(result.getFriendId()).isEqualTo(toUserId);
        
        then(userService).should().findUserById(fromUserId);
        then(userService).should().findUserById(toUserId);
        then(friendRepository).should().existsFriendshipBetweenUsers(fromUserId, toUserId);
        then(friendRepository).should().save(any(Friend.class));
    }

    @Test
    void shouldThrowInvalidOperationExceptionWhenSendingRequestToSelf() {
        // Given
        Long userId = 1L;

        // When & Then
        assertThatThrownBy(() -> friendService.sendFriendRequest(userId, userId))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("You cannot add yourself as a friend");

        then(friendRepository).should(never()).save(any(Friend.class));
    }

    @Test
    void shouldThrowInvalidOperationExceptionWhenFriendshipAlreadyExists() {
        // Given
        Long fromUserId = 1L;
        Long toUserId = 2L;
        
        User fromUser = User.builder().id(fromUserId).email("from@example.com").name("From User").build();
        User toUser = User.builder().id(toUserId).email("to@example.com").name("To User").build();
        
        given(userService.findUserById(fromUserId)).willReturn(fromUser);
        given(userService.findUserById(toUserId)).willReturn(toUser);
        given(friendRepository.existsFriendshipBetweenUsers(fromUserId, toUserId)).willReturn(true);

        // When & Then
        assertThatThrownBy(() -> friendService.sendFriendRequest(fromUserId, toUserId))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("Already friends with the user");
        
        then(userService).should().findUserById(fromUserId);
        then(userService).should().findUserById(toUserId);
        then(friendRepository).should().existsFriendshipBetweenUsers(fromUserId, toUserId);
        then(friendRepository).should(never()).save(any(Friend.class));
    }

    @Test
    void shouldAcceptFriendRequestSuccessfully() {
        // Given
        Long friendshipId = 10L;
        Long acceptingUserId = 2L; // The recipient accepting the request
        Long senderUserId = 1L;
        
        Friend pendingFriendship = Friend.builder()
                .id(friendshipId)
                .userId(senderUserId)
                .friendId(acceptingUserId)
                .status("PENDING")
                .build();
        
        Friend acceptedFriendship = Friend.builder()
                .id(friendshipId)
                .userId(senderUserId)
                .friendId(acceptingUserId)
                .status("ACCEPTED")
                .build();
        
        given(friendRepository.findById(friendshipId)).willReturn(Optional.of(pendingFriendship));
        given(friendRepository.save(any(Friend.class))).willReturn(acceptedFriendship);

        // When
        Friend result = friendService.acceptFriendRequest(friendshipId, acceptingUserId);

        // Then
        assertThat(result).isEqualTo(acceptedFriendship);
        assertThat(result.getStatus()).isEqualTo("ACCEPTED");
        
        then(friendRepository).should().findById(friendshipId);
        then(friendRepository).should().save(any(Friend.class));
    }

    @Test
    void shouldThrowInsufficientPermissionExceptionWhenWrongUserTriesToAccept() {
        // Given
        Long friendshipId = 10L;
        Long wrongUserId = 3L; // Someone else trying to accept
        Long acceptingUserId = 2L;
        Long senderUserId = 1L;
        
        Friend pendingFriendship = Friend.builder()
                .id(friendshipId)
                .userId(senderUserId)
                .friendId(acceptingUserId)
                .status("PENDING")
                .build();
        
        given(friendRepository.findById(friendshipId)).willReturn(Optional.of(pendingFriendship));

        // When & Then
        assertThatThrownBy(() -> friendService.acceptFriendRequest(friendshipId, wrongUserId))
                .isInstanceOf(InsufficientPermissionException.class)
                .hasMessageContaining("Only recipients can accept this friend request");
        
        then(friendRepository).should().findById(friendshipId);
        then(friendRepository).should(never()).save(any(Friend.class));
    }

    @Test
    void shouldThrowInvalidOperationExceptionWhenAcceptingNonPendingRequest() {
        // Given
        Long friendshipId = 10L;
        Long acceptingUserId = 2L;
        Long senderUserId = 1L;
        
        Friend acceptedFriendship = Friend.builder()
                .id(friendshipId)
                .userId(senderUserId)
                .friendId(acceptingUserId)
                .status("ACCEPTED") // Already accepted
                .build();
        
        given(friendRepository.findById(friendshipId)).willReturn(Optional.of(acceptedFriendship));

        // When & Then
        assertThatThrownBy(() -> friendService.acceptFriendRequest(friendshipId, acceptingUserId))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("Friend request is not pending");
        
        then(friendRepository).should().findById(friendshipId);
        then(friendRepository).should(never()).save(any(Friend.class));
    }

    @Test
    void shouldRejectFriendRequestSuccessfully() {
        // Given
        Long friendshipId = 10L;
        Long rejectingUserId = 2L;
        Long senderUserId = 1L;
        
        Friend pendingFriendship = Friend.builder()
                .id(friendshipId)
                .userId(senderUserId)
                .friendId(rejectingUserId)
                .status("PENDING")
                .build();
        
        Friend rejectedFriendship = Friend.builder()
                .id(friendshipId)
                .userId(senderUserId)
                .friendId(rejectingUserId)
                .status("REJECTED")
                .build();
        
        given(friendRepository.findById(friendshipId)).willReturn(Optional.of(pendingFriendship));
        given(friendRepository.save(any(Friend.class))).willReturn(rejectedFriendship);

        // When
        Friend result = friendService.rejectFriendRequest(friendshipId, rejectingUserId);

        // Then
        assertThat(result).isEqualTo(rejectedFriendship);
        assertThat(result.getStatus()).isEqualTo("REJECTED");
        
        then(friendRepository).should().findById(friendshipId);
        then(friendRepository).should().save(any(Friend.class));
    }

    @Test
    void shouldGetUserFriends() {
        // Given
        Long userId = 1L;
        
        Friend friendship1 = Friend.builder().id(1L).userId(userId).friendId(2L).status("ACCEPTED").build();
        Friend friendship2 = Friend.builder().id(2L).userId(userId).friendId(3L).status("ACCEPTED").build();

        User friend1 = User.builder().id(2L).email("friend1@example.com").name("Friend One").build();
        User friend2 = User.builder().id(3L).email("friend2@example.com").name("Friend Two").build();

        given(userService.findUserById(userId)).willReturn(User.builder().id(userId).build());
        given(friendRepository.findAcceptedFriendshipsForUser(userId)).willReturn(List.of(friendship1, friendship2));
        given(userService.findUserById(2L)).willReturn(friend1);
        given(userService.findUserById(3L)).willReturn(friend2);

        // When
        List<User> friends = friendService.getFriends(userId);

        // Then
        assertThat(friends).hasSize(2);
        assertThat(friends).contains(friend1, friend2);
        
        then(userService).should().findUserById(userId);
        then(friendRepository).should().findAcceptedFriendshipsForUser(userId);
    }

    @Test
    void shouldGetPendingFriendRequests() {
        // Given
        Long userId = 2L;
        
        Friend pendingRequest1 = Friend.builder()
                .id(10L)
                .userId(1L)
                .friendId(userId)
                .status("PENDING")
                .build();
        
        Friend pendingRequest2 = Friend.builder()
                .id(11L)
                .userId(3L)
                .friendId(userId)
                .status("PENDING")
                .build();
        
        given(userService.findUserById(userId)).willReturn(User.builder().id(userId).build());
        given(friendRepository.findByFriendIdAndStatus(userId, "PENDING")).willReturn(List.of(pendingRequest1, pendingRequest2));

        // When
        List<Friend> pendingRequests = friendService.getPendingRequests(userId);

        // Then
        assertThat(pendingRequests).hasSize(2);
        assertThat(pendingRequests).contains(pendingRequest1, pendingRequest2);
        
        then(userService).should().findUserById(userId);
        then(friendRepository).should().findByFriendIdAndStatus(userId, "PENDING");
    }
}