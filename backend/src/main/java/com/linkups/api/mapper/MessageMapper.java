package com.linkups.api.mapper;

import com.linkups.api.dto.response.chat.MessageListResponseDTO;
import com.linkups.api.dto.response.chat.MessageResponseDto;
import com.linkups.domain.entity.Message;
import org.springframework.data.domain.Page;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Message Mapper
 *
 * Utility class for converting between Message domain entities and DTOs.
 * Handles pagination and list conversions.
 *
 * @see Message
 * @see MessageResponseDto
 * @see MessageListResponseDTO
 */
public class MessageMapper {

    private MessageMapper() {
        throw new UnsupportedOperationException("MessageMapper is a utility class");
    }

    // ========== MESSAGE CONVERSIONS ==========

    public static MessageResponseDto toMessageResponse(Message message) {
        if (message == null) return null;

        return MessageResponseDto.builder()
                .id(message.getId())
                .senderId(message.getSenderId())
                .senderName(message.getSenderName())
                .roomId(message.getChatRoomId())
                .content(message.getContent())
                .messageType(message.getMessageType() != null ? message.getMessageType().toString() : "TEXT")
                .sentAt(message.getSentAt())
                .build();
    }

    public static List<MessageResponseDto> toMessageResponseList(List<Message> messages) {
        if (messages == null || messages.isEmpty()) {
            return Collections.emptyList();
        }

        return messages.stream()
                .map(MessageMapper::toMessageResponse)
                .collect(Collectors.toList());
    }

    public static MessageListResponseDTO toMessageListResponse(Page<Message> page) {
        if (page == null) {
            return MessageListResponseDTO.builder()
                    .messages(Collections.emptyList())
                    .totalElements(0L)
                    .totalPages(0)
                    .currentPage(0)
                    .size(0)
                    .unreadCount(0L)
                    .hasUnread(false)
                    .build();
        }

        List<MessageResponseDto> dtos = page.getContent().stream()
                .map(MessageMapper::toMessageResponse)
                .collect(Collectors.toList());

        return MessageListResponseDTO.builder()
                .messages(dtos)
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .currentPage(page.getNumber())
                .size(page.getSize())
                .unreadCount(0L)
                .hasUnread(false)
                .build();
    }

    public static MessageListResponseDTO toMessageListResponse(List<Message> messages) {
        if (messages == null || messages.isEmpty()) {
            return MessageListResponseDTO.builder()
                    .messages(Collections.emptyList())
                    .totalElements(0L)
                    .unreadCount(0L)
                    .hasUnread(false)
                    .build();
        }

        List<MessageResponseDto> dtos = toMessageResponseList(messages);

        return MessageListResponseDTO.builder()
                .messages(dtos)
                .totalElements((long) messages.size())
                .unreadCount(0L)
                .hasUnread(false)
                .build();
    }

    public static MessageListResponseDTO toMessageListResponseWithUnread(List<Message> messages, long unreadCount) {
        MessageListResponseDTO response = toMessageListResponse(messages);
        response.setUnreadCount(unreadCount);
        response.setHasUnread(unreadCount > 0);
        return response;
    }
}