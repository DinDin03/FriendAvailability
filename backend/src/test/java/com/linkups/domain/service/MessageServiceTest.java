package com.linkups.domain.service;

import com.linkups.base.BaseUnitTest;
import com.linkups.domain.entity.Message;
import com.linkups.domain.entity.ChatParticipant;
import com.linkups.domain.entity.enums.MessageType;
import com.linkups.domain.exception.InsufficientPermissionException;
import com.linkups.domain.exception.ValidationException;
import com.linkups.domain.repository.MessageRepository;
import com.linkups.domain.repository.ChatRoomRepository;
import com.linkups.domain.repository.ChatParticipantRepository;
import com.linkups.domain.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.*;

class MessageServiceTest extends BaseUnitTest {

    @Mock
    private MessageRepository messageRepository;
    
    @Mock
    private ChatRoomRepository chatRoomRepository;
    
    @Mock
    private ChatParticipantRepository chatParticipantRepository;
    
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private MessageService messageService;

    @Test
    void shouldSendMessageSuccessfully() {
        // Given
        Long senderId = 1L;
        Long roomId = 10L;
        String content = "Hello everyone!";
        
        
        Message savedMessage = Message.builder()
                .id(100L)
                .senderId(senderId)
                .chatRoomId(roomId)
                .content(content)
                .messageType(MessageType.TEXT)
                .sentAt(LocalDateTime.now())
                .build();
        
        given(userRepository.existsById(senderId)).willReturn(true);
        given(chatRoomRepository.existsById(roomId)).willReturn(true);
        given(userRepository.findById(senderId)).willReturn(Optional.of(
                com.linkups.domain.entity.User.builder().id(senderId).build()));
        given(chatRoomRepository.findById(roomId)).willReturn(Optional.of(
                com.linkups.domain.entity.ChatRoom.builder().id(roomId).build()));
        given(chatParticipantRepository.isUserActiveInRoom(senderId, roomId)).willReturn(true);
        given(messageRepository.save(any(Message.class))).willReturn(savedMessage);

        // When
        Message result = messageService.sendMessage(senderId, roomId, content);

        // Then
        assertThat(result).isEqualTo(savedMessage);
        assertThat(result.getContent()).isEqualTo(content);
        assertThat(result.getSenderId()).isEqualTo(senderId);
        assertThat(result.getChatRoomId()).isEqualTo(roomId);
        assertThat(result.getMessageType()).isEqualTo(MessageType.TEXT);

        then(userRepository).should().existsById(senderId);
        then(chatRoomRepository).should().existsById(roomId);
        then(userRepository).should().findById(senderId);
        then(chatRoomRepository).should(times(2)).findById(roomId);
        then(chatParticipantRepository).should().isUserActiveInRoom(senderId, roomId);
        then(messageRepository).should().save(any(Message.class));
    }

    @Test
    void shouldThrowValidationExceptionForEmptyContent() {
        // Given
        Long senderId = 1L;
        Long roomId = 10L;
        String emptyContent = "";

        given(userRepository.existsById(senderId)).willReturn(true);
        given(chatRoomRepository.existsById(roomId)).willReturn(true);
        given(chatParticipantRepository.isUserActiveInRoom(senderId, roomId)).willReturn(true);

        // When & Then
        assertThatThrownBy(() -> messageService.sendMessage(senderId, roomId, emptyContent))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Message content cannot be empty");
        
        then(messageRepository).should(never()).save(any(Message.class));
    }

    @Test
    void shouldThrowValidationExceptionForTooLongContent() {
        // Given
        Long senderId = 1L;
        Long roomId = 10L;
        String tooLongContent = "x".repeat(1001); // 1000 char limit

        given(userRepository.existsById(senderId)).willReturn(true);
        given(chatRoomRepository.existsById(roomId)).willReturn(true);
        given(chatParticipantRepository.isUserActiveInRoom(senderId, roomId)).willReturn(true);

        // When & Then
        assertThatThrownBy(() -> messageService.sendMessage(senderId, roomId, tooLongContent))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Message content cannot exceed 1000 characters");
        
        then(messageRepository).should(never()).save(any(Message.class));
    }

    @Test
    void shouldThrowInsufficientPermissionExceptionWhenUserNotParticipant() {
        // Given
        Long senderId = 1L;
        Long roomId = 10L;
        String content = "Hello!";
        
        
        given(userRepository.existsById(senderId)).willReturn(true);
        given(chatRoomRepository.existsById(roomId)).willReturn(true);
        given(chatParticipantRepository.isUserActiveInRoom(senderId, roomId)).willReturn(false);

        // When & Then
        assertThatThrownBy(() -> messageService.sendMessage(senderId, roomId, content))
                .isInstanceOf(InsufficientPermissionException.class)
                .hasMessageContaining("You don't have permission to send messages in this chat room");

        then(userRepository).should().existsById(senderId);
        then(chatRoomRepository).should().existsById(roomId);
        then(chatParticipantRepository).should().isUserActiveInRoom(senderId, roomId);
        then(messageRepository).should(never()).save(any(Message.class));
    }

    @Test
    void shouldGetMessageHistoryWithPagination() {
        // Given
        Long roomId = 10L;
        Long userId = 1L;
        int page = 0;
        int size = 20;
        
        Message message1 = Message.builder()
                .id(1L)
                .content("First message")
                .sentAt(LocalDateTime.now().minusMinutes(10))
                .build();
        
        Message message2 = Message.builder()
                .id(2L)
                .content("Second message")
                .sentAt(LocalDateTime.now().minusMinutes(5))
                .build();
        
        List<Message> messages = List.of(message2, message1); // Newest first
        Page<Message> messagePage = new PageImpl<>(messages, PageRequest.of(page, size), 2);

        given(userRepository.existsById(userId)).willReturn(true);
        given(chatRoomRepository.existsById(roomId)).willReturn(true);
        given(chatParticipantRepository.isUserActiveInRoom(userId, roomId)).willReturn(true);
        given(messageRepository.findByChatRoomIdOrderBySentAtDesc(eq(roomId), any(Pageable.class)))
                .willReturn(messagePage);

        // When
        Page<Message> result = messageService.getMessageHistory(roomId, userId, page, size);

        // Then
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent().get(0)).isEqualTo(message2); // Newest first
        assertThat(result.getContent().get(1)).isEqualTo(message1);
        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getTotalPages()).isEqualTo(1);

        then(userRepository).should().existsById(userId);
        then(chatRoomRepository).should().existsById(roomId);
        then(chatParticipantRepository).should().isUserActiveInRoom(userId, roomId);
        then(messageRepository).should().findByChatRoomIdOrderBySentAtDesc(eq(roomId), any(Pageable.class));
    }

    @Test
    void shouldThrowInsufficientPermissionExceptionWhenGettingHistoryAsNonParticipant() {
        // Given
        Long roomId = 10L;
        Long userId = 1L;
        int page = 0;
        int size = 20;

        given(userRepository.existsById(userId)).willReturn(true);
        given(chatRoomRepository.existsById(roomId)).willReturn(true);
        given(chatParticipantRepository.isUserActiveInRoom(userId, roomId)).willReturn(false);

        // When & Then
        assertThatThrownBy(() -> messageService.getMessageHistory(roomId, userId, page, size))
                .isInstanceOf(InsufficientPermissionException.class)
                .hasMessageContaining("You don't have permission to send messages in this chat room");

        then(userRepository).should().existsById(userId);
        then(chatRoomRepository).should().existsById(roomId);
        then(chatParticipantRepository).should().isUserActiveInRoom(userId, roomId);
        then(messageRepository).should(never()).findByChatRoomIdOrderBySentAtDesc(any(), any());
    }

    @Test
    void shouldGetRecentMessages() {
        // Given
        Long roomId = 10L;
        Long userId = 1L;
        int limit = 50;
        
        Message recentMessage1 = Message.builder()
                .id(1L)
                .content("Recent message 1")
                .sentAt(LocalDateTime.now().minusMinutes(2))
                .build();
        
        Message recentMessage2 = Message.builder()
                .id(2L)
                .content("Recent message 2")
                .sentAt(LocalDateTime.now().minusMinutes(1))
                .build();
        
        List<Message> recentMessages = List.of(recentMessage2, recentMessage1);

        given(userRepository.existsById(userId)).willReturn(true);
        given(chatRoomRepository.existsById(roomId)).willReturn(true);
        given(chatParticipantRepository.isUserActiveInRoom(userId, roomId)).willReturn(true);
        given(messageRepository.findRecentMessagesInRoom(roomId, limit)).willReturn(recentMessages);

        // When
        List<Message> result = messageService.getRecentMessages(roomId, userId, limit);

        // Then
        assertThat(result).hasSize(2);
        assertThat(result).containsExactly(recentMessage2, recentMessage1);

        then(userRepository).should().existsById(userId);
        then(chatRoomRepository).should().existsById(roomId);
        then(chatParticipantRepository).should().isUserActiveInRoom(userId, roomId);
        then(messageRepository).should().findRecentMessagesInRoom(roomId, limit);
    }

    @Test
    void shouldSearchMessages() {
        // Given
        Long roomId = 10L;
        Long userId = 1L;
        String searchTerm = "important";
        
        Message matchingMessage1 = Message.builder()
                .id(1L)
                .content("This is an important message")
                .build();
        
        Message matchingMessage2 = Message.builder()
                .id(2L)
                .content("Another important update")
                .build();
        
        List<Message> searchResults = List.of(matchingMessage1, matchingMessage2);

        given(userRepository.existsById(userId)).willReturn(true);
        given(chatRoomRepository.existsById(roomId)).willReturn(true);
        given(chatParticipantRepository.isUserActiveInRoom(userId, roomId)).willReturn(true);
        given(messageRepository.searchMessagesInRoom(roomId, searchTerm)).willReturn(searchResults);

        // When
        List<Message> result = messageService.searchMessages(roomId, userId, searchTerm);

        // Then
        assertThat(result).hasSize(2);
        assertThat(result).containsExactly(matchingMessage1, matchingMessage2);

        then(userRepository).should().existsById(userId);
        then(chatRoomRepository).should().existsById(roomId);
        then(chatParticipantRepository).should().isUserActiveInRoom(userId, roomId);
        then(messageRepository).should().searchMessagesInRoom(roomId, searchTerm);
    }

    @Test
    void shouldGetUnreadMessagesCount() {
        // Given
        Long roomId = 10L;
        Long userId = 1L;
        long expectedCount = 5L;

        ChatParticipant participant = ChatParticipant.builder()
                .userId(userId)
                .chatRoomId(roomId)
                .lastReadAt(LocalDateTime.now().minusHours(1))
                .build();

        given(userRepository.existsById(userId)).willReturn(true);
        given(chatRoomRepository.existsById(roomId)).willReturn(true);
        given(chatParticipantRepository.isUserActiveInRoom(userId, roomId)).willReturn(true);
        given(chatParticipantRepository.findActiveParticipation(userId, roomId)).willReturn(Optional.of(participant));
        given(messageRepository.countUnreadMessages(eq(roomId), eq(userId), any(LocalDateTime.class))).willReturn(expectedCount);

        // When
        long result = messageService.getUnreadMessagesCount(roomId, userId);

        // Then
        assertThat(result).isEqualTo(expectedCount);

        then(userRepository).should().existsById(userId);
        then(chatRoomRepository).should().existsById(roomId);
        then(chatParticipantRepository).should().isUserActiveInRoom(userId, roomId);
        then(chatParticipantRepository).should().findActiveParticipation(userId, roomId);
        then(messageRepository).should().countUnreadMessages(eq(roomId), eq(userId), any(LocalDateTime.class));
    }

    @Test
    void shouldMarkMessagesAsRead() {
        // Given
        Long roomId = 10L;
        Long userId = 1L;

        given(userRepository.existsById(userId)).willReturn(true);
        given(chatRoomRepository.existsById(roomId)).willReturn(true);
        given(chatParticipantRepository.isUserActiveInRoom(userId, roomId)).willReturn(true);

        // When
        messageService.markMessagesAsRead(roomId, userId);

        // Then
        then(userRepository).should().existsById(userId);
        then(chatRoomRepository).should().existsById(roomId);
        then(chatParticipantRepository).should().isUserActiveInRoom(userId, roomId);
        then(chatParticipantRepository).should().updateLastReadTime(eq(userId), eq(roomId), any(LocalDateTime.class));
    }
}