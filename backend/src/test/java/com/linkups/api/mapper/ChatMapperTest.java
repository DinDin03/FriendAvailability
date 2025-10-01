package com.linkups.api.mapper;

import com.linkups.api.dto.response.chat.*;
import com.linkups.base.BaseUnitTest;
import com.linkups.domain.entity.ChatParticipant;
import com.linkups.domain.entity.ChatRoom;
import com.linkups.domain.entity.Message;
import com.linkups.domain.entity.User;
import com.linkups.domain.entity.enums.ChatType;
import com.linkups.domain.entity.enums.MessageType;
import com.linkups.domain.entity.enums.ParticipantRole;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ChatMapperTest extends BaseUnitTest {

    @Test
    void toChatRoomResponse_withValidChatRoom_shouldMapCorrectly() {
        // Given
        ChatRoom chatRoom = createChatRoom(1L, "Test Room", ChatType.GROUP);
        Long userId = 100L;
        Long unreadCount = 5L;

        // When
        ChatRoomResponseDTO result = ChatMapper.toChatRoomResponse(chatRoom, userId, unreadCount);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo("Test Room");
        assertThat(result.getType()).isEqualTo("GROUP");
        assertThat(result.getUnreadCount()).isEqualTo(5L);
        assertThat(result.getParticipantCount()).isEqualTo(0);
    }

    @Test
    void toChatRoomResponse_withNullChatRoom_shouldReturnNull() {
        // When
        ChatRoomResponseDTO result = ChatMapper.toChatRoomResponse(null, 1L, 0L);

        // Then
        assertThat(result).isNull();
    }

    @Test
    void toChatRoomSummary_withValidChatRoom_shouldMapCorrectly() {
        // Given
        ChatRoom chatRoom = createChatRoom(1L, "Test Room", ChatType.GROUP);
        Message lastMessage = createMessage(1L, 1L, 100L, "Last message");
        chatRoom.setMessages(Collections.singletonList(lastMessage));
        Long unreadCount = 3L;

        // When
        ChatRoomSummaryDTO result = ChatMapper.toChatRoomSummary(chatRoom, 1L, unreadCount);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getDisplayName()).isEqualTo("Test Room");
        assertThat(result.getType()).isEqualTo("GROUP");
        assertThat(result.getUnreadCount()).isEqualTo(3L);
        assertThat(result.getLastMessagePreview()).isEqualTo("Last message");
        assertThat(result.getIsActive()).isTrue();
    }

    @Test
    void toChatRoomSummary_withLongMessage_shouldTruncate() {
        // Given
        ChatRoom chatRoom = createChatRoom(1L, "Test Room", ChatType.GROUP);
        String longContent = "This is a very long message that should be truncated to 50 characters";
        Message lastMessage = createMessage(1L, 1L, 100L, longContent);
        chatRoom.setMessages(Collections.singletonList(lastMessage));

        // When
        ChatRoomSummaryDTO result = ChatMapper.toChatRoomSummary(chatRoom, 1L, 0L);

        // Then
        assertThat(result.getLastMessagePreview()).hasSize(50);
        assertThat(result.getLastMessagePreview()).endsWith("...");
    }

    @Test
    void toChatRoomListResponse_withPagedResults_shouldMapCorrectly() {
        // Given
        List<ChatRoom> rooms = Arrays.asList(
                createChatRoom(1L, "Room 1", ChatType.PRIVATE),
                createChatRoom(2L, "Room 2", ChatType.GROUP)
        );
        Page<ChatRoom> page = new PageImpl<>(rooms, PageRequest.of(0, 10), 2);
        Long userId = 100L;

        // When
        ChatRoomListResponseDTO result = ChatMapper.toChatRoomListResponse(page, userId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getChatRooms()).hasSize(2);
        assertThat(result.getTotalElements()).isEqualTo(2L);
        assertThat(result.getTotalPages()).isEqualTo(1);
        assertThat(result.getCurrentPage()).isEqualTo(0);
    }

    @Test
    void toChatRoomListResponse_withEmptyPage_shouldReturnEmptyResponse() {
        // Given
        Page<ChatRoom> emptyPage = new PageImpl<>(Collections.emptyList());

        // When
        ChatRoomListResponseDTO result = ChatMapper.toChatRoomListResponse(emptyPage, 100L);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getChatRooms()).isEmpty();
        assertThat(result.getTotalElements()).isEqualTo(0L);
    }

    @Test
    void toChatRoomListResponse_withList_shouldMapCorrectly() {
        // Given
        List<ChatRoom> rooms = Arrays.asList(
                createChatRoom(1L, "Room 1", ChatType.PRIVATE),
                createChatRoom(2L, "Room 2", ChatType.GROUP)
        );

        // When
        ChatRoomListResponseDTO result = ChatMapper.toChatRoomListResponse(rooms, 1L);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getChatRooms()).hasSize(2);
        assertThat(result.getTotalElements()).isEqualTo(2L);
    }

    @Test
    void toParticipantDTO_withValidParticipant_shouldMapCorrectly() {
        // Given
        ChatParticipant participant = createParticipant(1L, 100L, "John Doe", ParticipantRole.ADMIN);

        // When
        ChatParticipantDTO result = ChatMapper.toParticipantDTO(participant);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getUserId()).isEqualTo(100L);
        assertThat(result.getUserName()).isEqualTo("John Doe");
        assertThat(result.getRole()).isEqualTo("ADMIN");
        assertThat(result.getIsActive()).isTrue();
        assertThat(result.getIsOnline()).isFalse();
    }

    @Test
    void toParticipantDTO_withNullParticipant_shouldReturnNull() {
        // When
        ChatParticipantDTO result = ChatMapper.toParticipantDTO(null);

        // Then
        assertThat(result).isNull();
    }

    @Test
    void toParticipantListResponse_withValidParticipants_shouldMapCorrectly() {
        // Given
        List<ChatParticipant> participants = Arrays.asList(
                createParticipant(1L, 100L, "Admin User", ParticipantRole.ADMIN),
                createParticipant(2L, 101L, "Member User", ParticipantRole.MEMBER),
                createParticipant(3L, 102L, "Another Member", ParticipantRole.MEMBER)
        );

        // When
        ChatParticipantListResponseDTO result = ChatMapper.toParticipantListResponse(participants);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getParticipants()).hasSize(3);
        assertThat(result.getTotalParticipants()).isEqualTo(3);
        assertThat(result.getAdminCount()).isEqualTo(1);
        assertThat(result.getMemberCount()).isEqualTo(2);
        assertThat(result.getActiveCount()).isEqualTo(3);
    }

    @Test
    void toParticipantListResponse_withEmptyList_shouldReturnEmptyResponse() {
        // When
        ChatParticipantListResponseDTO result = ChatMapper.toParticipantListResponse(Collections.emptyList());

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getParticipants()).isEmpty();
        assertThat(result.getTotalParticipants()).isEqualTo(0);
        assertThat(result.getAdminCount()).isEqualTo(0);
        assertThat(result.getMemberCount()).isEqualTo(0);
    }

    // Helper methods
    private ChatRoom createChatRoom(Long id, String name, ChatType type) {
        ChatRoom room = new ChatRoom();
        room.setId(id);
        room.setName(name);
        room.setType(type);
        room.setCreatedBy(1L);
        room.setCreatedAt(LocalDateTime.now());
        room.setUpdatedAt(LocalDateTime.now());
        room.setParticipants(new ArrayList<>());
        room.setMessages(new ArrayList<>());
        return room;
    }

    private ChatParticipant createParticipant(Long id, Long userId, String userName, ParticipantRole role) {
        User user = new User();
        user.setId(userId);
        user.setName(userName);
        user.setEmail(userName.toLowerCase().replace(" ", ".") + "@test.com");

        ChatParticipant participant = new ChatParticipant();
        participant.setId(id);
        participant.setUserId(userId);
        participant.setUser(user);
        participant.setRole(role);
        participant.setJoinedAt(LocalDateTime.now());
        participant.setIsActive(true);
        return participant;
    }

    private Message createMessage(Long id, Long roomId, Long senderId, String content) {
        User sender = new User();
        sender.setId(senderId);
        sender.setName("Test User");

        Message message = new Message();
        message.setId(id);
        message.setChatRoomId(roomId);
        message.setSenderId(senderId);
        message.setSender(sender);
        message.setContent(content);
        message.setMessageType(MessageType.TEXT);
        message.setSentAt(LocalDateTime.now());
        return message;
    }
}