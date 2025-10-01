package com.linkups.domain.service;

import com.linkups.base.BaseUnitTest;
import com.linkups.domain.entity.ChatRoom;
import com.linkups.domain.entity.ChatParticipant;
import com.linkups.domain.entity.User;
import com.linkups.domain.entity.enums.ChatType;
import com.linkups.domain.entity.enums.ParticipantRole;
import com.linkups.domain.exception.InsufficientPermissionException;
import com.linkups.domain.exception.InvalidOperationException;
import com.linkups.domain.exception.ValidationException;
import com.linkups.domain.repository.ChatRoomRepository;
import com.linkups.domain.repository.ChatParticipantRepository;
import com.linkups.domain.repository.MessageRepository;
import com.linkups.domain.repository.FriendRepository;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.*;

//all tests passing
class ChatServiceTest extends BaseUnitTest {

    @Mock
    private ChatRoomRepository chatRoomRepository;
    
    @Mock
    private ChatParticipantRepository chatParticipantRepository;
    
    @Mock
    private MessageRepository messageRepository;
    
    @Mock
    private FriendRepository friendRepository;
    
    @Mock
    private UserService userService;

    @InjectMocks
    private ChatService chatService;

    @Test
    void shouldCreateGroupChatSuccessfully() {
        // Given
        Long creatorId = 1L;
        String chatName = "Team Project";
        List<Long> participantIds = List.of(2L, 3L, 4L);

        User creator = User.builder().id(creatorId).name("Creator").build();
        User participant2 = User.builder().id(2L).name("User 2").build();
        User participant3 = User.builder().id(3L).name("User 3").build();
        User participant4 = User.builder().id(4L).name("User 4").build();

        ChatRoom savedChatRoom = ChatRoom.builder()
                .id(10L)
                .name(chatName)
                .type(ChatType.GROUP)
                .createdBy(creatorId)
                .build();

        given(userService.findUserById(creatorId)).willReturn(creator);
        given(userService.findUserById(2L)).willReturn(participant2);
        given(userService.findUserById(3L)).willReturn(participant3);
        given(userService.findUserById(4L)).willReturn(participant4);
        given(chatRoomRepository.save(any(ChatRoom.class))).willReturn(savedChatRoom);
        given(chatRoomRepository.findById(10L)).willReturn(Optional.of(savedChatRoom));
        given(chatParticipantRepository.userExistsInRoom(any(Long.class), eq(10L))).willReturn(false);

        // When
        ChatRoom result = chatService.createGroupChat(creatorId, chatName, participantIds);

        // Then
        assertThat(result).isEqualTo(savedChatRoom);
        assertThat(result.getName()).isEqualTo(chatName);
        assertThat(result.getType()).isEqualTo(ChatType.GROUP);
        assertThat(result.getCreatedBy()).isEqualTo(creatorId);

        then(userService).should(times(2)).findUserById(creatorId);
        then(userService).should(times(2)).findUserById(2L);
        then(userService).should(times(2)).findUserById(3L);
        then(userService).should(times(2)).findUserById(4L);
        then(chatRoomRepository).should().save(any(ChatRoom.class));

        // Verify participants are added (creator as ADMIN, others as MEMBER)
        then(chatParticipantRepository).should(times(4)).save(any(ChatParticipant.class));
    }

    @Test
    void shouldThrowValidationExceptionForEmptyGroupName() {
        // Given
        Long creatorId = 1L;
        String emptyName = "";
        List<Long> participantIds = List.of(2L, 3L);

        // When & Then
        assertThatThrownBy(() -> chatService.createGroupChat(creatorId, emptyName, participantIds))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Chat room name is required");
        
        then(chatRoomRepository).should(never()).save(any(ChatRoom.class));
    }

    @Test
    void shouldThrowValidationExceptionForTooFewParticipants() {
        // Given
        Long creatorId = 1L;
        String chatName = "Small Group";
        List<Long> participantIds = List.of(); // No participants besides creator

        // When & Then
        assertThatThrownBy(() -> chatService.createGroupChat(creatorId, chatName, participantIds))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("Group chats require at least 2 participants");
        
        then(chatRoomRepository).should(never()).save(any(ChatRoom.class));
    }

    @Test
    void shouldCreatePrivateChatSuccessfully() {
        // Given
        Long userId1 = 1L;
        Long userId2 = 2L;
        
        User user1 = User.builder().id(userId1).name("User 1").build();
        User user2 = User.builder().id(userId2).name("User 2").build();
        
        ChatRoom savedChatRoom = ChatRoom.builder()
                .id(20L)
                .name(null) // Private chats don't have names
                .type(ChatType.PRIVATE)
                .createdBy(userId1)
                .build();

        given(userService.findUserById(userId1)).willReturn(user1);
        given(userService.findUserById(userId2)).willReturn(user2);
        given(chatRoomRepository.findPrivateChatBetweenUsers(userId1, userId2))
                .willReturn(Optional.empty());
        given(chatRoomRepository.save(any(ChatRoom.class))).willReturn(savedChatRoom);
        given(chatRoomRepository.findById(20L)).willReturn(Optional.of(savedChatRoom));

        // When
        ChatRoom result = chatService.getOrCreatePrivateChat(userId1, userId2);

        // Then
        assertThat(result).isEqualTo(savedChatRoom);
        assertThat(result.getType()).isEqualTo(ChatType.PRIVATE);
        assertThat(result.getName()).isNull();

        then(userService).should(times(2)).findUserById(userId1);
        then(userService).should(times(2)).findUserById(userId2);
        then(chatRoomRepository).should().findPrivateChatBetweenUsers(userId1, userId2);
        then(chatRoomRepository).should().save(any(ChatRoom.class));
        then(chatParticipantRepository).should(times(2)).save(any(ChatParticipant.class));
    }

    @Test
    void shouldReturnExistingPrivateChat() {
        // Given
        Long userId1 = 1L;
        Long userId2 = 2L;
        
        User user1 = User.builder().id(userId1).name("User 1").build();
        User user2 = User.builder().id(userId2).name("User 2").build();
        
        ChatRoom existingChatRoom = ChatRoom.builder()
                .id(20L)
                .type(ChatType.PRIVATE)
                .createdBy(userId1)
                .build();
        
        given(userService.findUserById(userId1)).willReturn(user1);
        given(userService.findUserById(userId2)).willReturn(user2);
        given(chatRoomRepository.findPrivateChatBetweenUsers(userId1, userId2))
                .willReturn(Optional.of(existingChatRoom));

        // When
        ChatRoom result = chatService.getOrCreatePrivateChat(userId1, userId2);

        // Then
        assertThat(result).isEqualTo(existingChatRoom);
        
        then(userService).should().findUserById(userId1);
        then(userService).should().findUserById(userId2);
        then(chatRoomRepository).should().findPrivateChatBetweenUsers(userId1, userId2);
        then(chatRoomRepository).should(never()).save(any(ChatRoom.class));
    }


    @Test
    void shouldGetUserChatRoomsWithPagination() {
        // Given
        Long userId = 1L;
        int page = 0;
        int size = 10;
        
        ChatRoom room1 = ChatRoom.builder().id(1L).name("Room 1").type(ChatType.GROUP).build();
        ChatRoom room2 = ChatRoom.builder().id(2L).name("Room 2").type(ChatType.GROUP).build();
        
        List<ChatRoom> rooms = List.of(room1, room2);
        Page<ChatRoom> roomPage = new PageImpl<>(rooms, PageRequest.of(page, size), 2);
        
        given(userService.findUserById(userId)).willReturn(User.builder().id(userId).build());
        given(chatRoomRepository.findChatRoomsForUser(eq(userId), any(Pageable.class)))
                .willReturn(roomPage);

        // When
        Page<ChatRoom> result = chatService.getUserChatRooms(userId, page, size);

        // Then
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent()).containsExactly(room1, room2);
        assertThat(result.getTotalElements()).isEqualTo(2);
        
        then(userService).should().findUserById(userId);
        then(chatRoomRepository).should().findChatRoomsForUser(eq(userId), any(Pageable.class));
    }

    @Test
    void shouldGetChatRoomDetails() {
        // Given
        Long roomId = 10L;
        Long userId = 1L;
        
        ChatRoom chatRoom = ChatRoom.builder()
                .id(roomId)
                .name("Test Room")
                .type(ChatType.GROUP)
                .build();
        
        given(chatRoomRepository.findById(roomId)).willReturn(Optional.of(chatRoom));
        given(chatParticipantRepository.userExistsInRoom(userId, roomId)).willReturn(true);

        // When
        ChatRoom result = chatService.getChatRoom(roomId, userId);

        // Then
        assertThat(result).isEqualTo(chatRoom);

        then(chatRoomRepository).should().findById(roomId);
        then(chatParticipantRepository).should().userExistsInRoom(userId, roomId);
    }

    @Test
    void shouldThrowInsufficientPermissionExceptionWhenNonParticipantAccessesRoom() {
        // Given
        Long roomId = 10L;
        Long userId = 1L;
        
        ChatRoom chatRoom = ChatRoom.builder().id(roomId).build();
        
        given(chatRoomRepository.findById(roomId)).willReturn(Optional.of(chatRoom));
        given(chatParticipantRepository.userExistsInRoom(userId, roomId)).willReturn(false);

        // When & Then
        assertThatThrownBy(() -> chatService.getChatRoom(roomId, userId))
                .isInstanceOf(InsufficientPermissionException.class)
                .hasMessageContaining("You don't have permission to access this chat room");

        then(chatRoomRepository).should().findById(roomId);
        then(chatParticipantRepository).should().userExistsInRoom(userId, roomId);
    }

    @Test
    void shouldAddUserToGroupChatAsAdmin() {
        // Given
        Long roomId = 10L;
        Long newUserId = 3L;
        Long adminUserId = 1L;

        User newUser = User.builder().id(newUserId).name("New User").build();
        ChatRoom chatRoom = ChatRoom.builder().id(roomId).type(ChatType.GROUP).build();
        ChatParticipant adminParticipant = ChatParticipant.builder()
                .userId(adminUserId)
                .chatRoomId(roomId)
                .role(ParticipantRole.ADMIN)
                .build();

        given(userService.findUserById(newUserId)).willReturn(newUser);
        given(chatRoomRepository.findById(roomId)).willReturn(Optional.of(chatRoom));
        given(chatParticipantRepository.findActiveParticipation(adminUserId, roomId))
                .willReturn(Optional.of(adminParticipant));
        given(chatParticipantRepository.userExistsInRoom(newUserId, roomId)).willReturn(false);

        // When
        chatService.addUserToGroupChat(roomId, newUserId, adminUserId);

        // Then
        then(userService).should(times(2)).findUserById(newUserId);
        then(chatRoomRepository).should(times(2)).findById(roomId);
        then(chatParticipantRepository).should().findActiveParticipation(adminUserId, roomId);
        then(chatParticipantRepository).should(times(2)).userExistsInRoom(newUserId, roomId);
        then(chatParticipantRepository).should().save(any(ChatParticipant.class));
    }

    @Test
    void shouldThrowInsufficientPermissionExceptionWhenNonAdminAddsUser() {
        // Given
        Long roomId = 10L;
        Long newUserId = 3L;
        Long regularUserId = 1L;

        ChatRoom chatRoom = ChatRoom.builder().id(roomId).build();
        ChatParticipant regularParticipant = ChatParticipant.builder()
                .userId(regularUserId)
                .chatRoomId(roomId)
                .role(ParticipantRole.MEMBER)
                .build();

        given(chatRoomRepository.findById(roomId))
                .willReturn(Optional.of(chatRoom));
        given(chatParticipantRepository.findActiveParticipation(regularUserId, roomId))
                .willReturn(Optional.of(regularParticipant));

        // When & Then
        assertThatThrownBy(() -> chatService.addUserToGroupChat(roomId, newUserId, regularUserId))
                .isInstanceOf(InsufficientPermissionException.class)
                .hasMessageContaining("Only group admins can add users to this group chat");
        
        then(chatParticipantRepository).should().findActiveParticipation(regularUserId, roomId);
        then(chatParticipantRepository).should(never()).save(any(ChatParticipant.class));
    }

    @Test
    void shouldGetChatParticipants() {
        // Given
        Long roomId = 10L;
        Long userId = 1L;
        
        ChatParticipant admin = ChatParticipant.builder()
                .userId(1L)
                .chatRoomId(roomId)
                .role(ParticipantRole.ADMIN)
                .build();
        
        ChatParticipant member = ChatParticipant.builder()
                .userId(2L)
                .chatRoomId(roomId)
                .role(ParticipantRole.MEMBER)
                .build();
        
        List<ChatParticipant> participants = List.of(admin, member);
        
        given(chatRoomRepository.findById(roomId)).willReturn(Optional.of(ChatRoom.builder().id(roomId).build()));
        given(chatParticipantRepository.userExistsInRoom(userId, roomId)).willReturn(true);
        given(chatParticipantRepository.findByChatRoomIdAndIsActiveTrue(roomId)).willReturn(participants);

        // When
        List<ChatParticipant> result = chatService.getChatParticipants(roomId, userId);

        // Then
        assertThat(result).hasSize(2);
        assertThat(result).containsExactly(admin, member);
        
        then(chatRoomRepository).should().findById(roomId);
        then(chatParticipantRepository).should().userExistsInRoom(userId, roomId);
        then(chatParticipantRepository).should().findByChatRoomIdAndIsActiveTrue(roomId);
    }
}