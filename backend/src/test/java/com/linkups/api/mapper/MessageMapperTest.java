package com.linkups.api.mapper;

import com.linkups.api.dto.response.chat.MessageListResponseDTO;
import com.linkups.api.dto.response.chat.MessageResponseDto;
import com.linkups.base.BaseUnitTest;
import com.linkups.domain.entity.Message;
import com.linkups.domain.entity.User;
import com.linkups.domain.entity.enums.MessageType;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MessageMapperTest extends BaseUnitTest {

    @Test
    void toMessageResponse_withValidMessage_shouldMapCorrectly() {
        // Given
        Message message = createMessage(1L, 100L, 200L, "Test message");

        // When
        MessageResponseDto result = MessageMapper.toMessageResponse(message);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getSenderId()).isEqualTo(200L);
        assertThat(result.getSenderName()).isEqualTo("Test Sender");
        assertThat(result.getRoomId()).isEqualTo(100L);
        assertThat(result.getContent()).isEqualTo("Test message");
        assertThat(result.getMessageType()).isEqualTo("TEXT");
        assertThat(result.getSentAt()).isNotNull();
    }

    @Test
    void toMessageResponse_withNullMessage_shouldReturnNull() {
        // When
        MessageResponseDto result = MessageMapper.toMessageResponse(null);

        // Then
        assertThat(result).isNull();
    }

    @Test
    void toMessageResponseList_withValidMessages_shouldMapCorrectly() {
        // Given
        List<Message> messages = Arrays.asList(
                createMessage(1L, 100L, 200L, "Message 1"),
                createMessage(2L, 100L, 201L, "Message 2"),
                createMessage(3L, 100L, 202L, "Message 3")
        );

        // When
        List<MessageResponseDto> result = MessageMapper.toMessageResponseList(messages);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(3);
        assertThat(result.get(0).getContent()).isEqualTo("Message 1");
        assertThat(result.get(1).getContent()).isEqualTo("Message 2");
        assertThat(result.get(2).getContent()).isEqualTo("Message 3");
    }

    @Test
    void toMessageResponseList_withEmptyList_shouldReturnEmptyList() {
        // When
        List<MessageResponseDto> result = MessageMapper.toMessageResponseList(Collections.emptyList());

        // Then
        assertThat(result).isNotNull();
        assertThat(result).isEmpty();
    }

    @Test
    void toMessageResponseList_withNull_shouldReturnEmptyList() {
        // When
        List<MessageResponseDto> result = MessageMapper.toMessageResponseList(null);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).isEmpty();
    }

    @Test
    void toMessageListResponse_withPagedResults_shouldMapCorrectly() {
        // Given
        List<Message> messages = Arrays.asList(
                createMessage(1L, 100L, 200L, "Message 1"),
                createMessage(2L, 100L, 201L, "Message 2")
        );
        Page<Message> page = new PageImpl<>(messages, PageRequest.of(0, 20), 2);

        // When
        MessageListResponseDTO result = MessageMapper.toMessageListResponse(page);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getMessages()).hasSize(2);
        assertThat(result.getTotalElements()).isEqualTo(2L);
        assertThat(result.getTotalPages()).isEqualTo(1);
        assertThat(result.getCurrentPage()).isEqualTo(0);
        assertThat(result.getSize()).isEqualTo(20);
        assertThat(result.getUnreadCount()).isEqualTo(0L);
        assertThat(result.getHasUnread()).isFalse();
    }

    @Test
    void toMessageListResponse_withEmptyPage_shouldReturnEmptyResponse() {
        // Given
        Page<Message> emptyPage = new PageImpl<>(Collections.emptyList());

        // When
        MessageListResponseDTO result = MessageMapper.toMessageListResponse(emptyPage);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getMessages()).isEmpty();
        assertThat(result.getTotalElements()).isEqualTo(0L);
    }

    @Test
    void toMessageListResponse_withNullPage_shouldReturnEmptyResponse() {
        // When
        MessageListResponseDTO result = MessageMapper.toMessageListResponse((Page<Message>) null);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getMessages()).isEmpty();
        assertThat(result.getTotalElements()).isEqualTo(0L);
        assertThat(result.getTotalPages()).isEqualTo(0);
    }

    @Test
    void toMessageListResponse_withList_shouldMapCorrectly() {
        // Given
        List<Message> messages = Arrays.asList(
                createMessage(1L, 100L, 200L, "Message 1"),
                createMessage(2L, 100L, 201L, "Message 2"),
                createMessage(3L, 100L, 202L, "Message 3")
        );

        // When
        MessageListResponseDTO result = MessageMapper.toMessageListResponse(messages);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getMessages()).hasSize(3);
        assertThat(result.getTotalElements()).isEqualTo(3L);
        assertThat(result.getUnreadCount()).isEqualTo(0L);
        assertThat(result.getHasUnread()).isFalse();
    }

    @Test
    void toMessageListResponse_withEmptyList_shouldReturnEmptyResponse() {
        // When
        MessageListResponseDTO result = MessageMapper.toMessageListResponse(Collections.emptyList());

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getMessages()).isEmpty();
        assertThat(result.getTotalElements()).isEqualTo(0L);
    }

    @Test
    void toMessageListResponseWithUnread_shouldSetUnreadCountCorrectly() {
        // Given
        List<Message> messages = Arrays.asList(
                createMessage(1L, 100L, 200L, "Message 1"),
                createMessage(2L, 100L, 201L, "Message 2")
        );
        long unreadCount = 5L;

        // When
        MessageListResponseDTO result = MessageMapper.toMessageListResponseWithUnread(messages, unreadCount);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getMessages()).hasSize(2);
        assertThat(result.getUnreadCount()).isEqualTo(5L);
        assertThat(result.getHasUnread()).isTrue();
    }

    @Test
    void toMessageListResponseWithUnread_withZeroUnread_shouldSetHasUnreadToFalse() {
        // Given
        List<Message> messages = Arrays.asList(
                createMessage(1L, 100L, 200L, "Message 1")
        );

        // When
        MessageListResponseDTO result = MessageMapper.toMessageListResponseWithUnread(messages, 0L);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getUnreadCount()).isEqualTo(0L);
        assertThat(result.getHasUnread()).isFalse();
    }

    @Test
    void toMessageResponse_withDifferentMessageTypes_shouldMapCorrectly() {
        // Given
        Message systemMessage = createMessage(1L, 100L, 200L, "User joined");
        systemMessage.setMessageType(MessageType.SYSTEM_MESSAGE);

        // When
        MessageResponseDto result = MessageMapper.toMessageResponse(systemMessage);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getMessageType()).isEqualTo("SYSTEM_MESSAGE");
    }

    // Helper methods
    private Message createMessage(Long id, Long roomId, Long senderId, String content) {
        User sender = new User();
        sender.setId(senderId);
        sender.setName("Test Sender");

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