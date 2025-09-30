package com.linkups.api.mapper;

import com.linkups.api.dto.response.chat.*;
import com.linkups.domain.entity.ChatParticipant;
import com.linkups.domain.entity.ChatRoom;
import com.linkups.domain.entity.Message;
import com.linkups.domain.entity.enums.ParticipantRole;
import org.springframework.data.domain.Page;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Chat Mapper
 *
 * Utility class for converting between ChatRoom/ChatParticipant domain entities and DTOs.
 * Handles circular reference prevention and null safety.
 *
 * @see ChatRoom
 * @see ChatParticipant
 * @see ChatRoomResponseDTO
 * @see ChatParticipantDTO
 */
public class ChatMapper {

    private ChatMapper() {
        throw new UnsupportedOperationException("ChatMapper is a utility class");
    }

    // ========== CHAT ROOM CONVERSIONS ==========

    public static ChatRoomResponseDTO toChatRoomResponse(ChatRoom chatRoom, Long userId, Long unreadCount) {
        if (chatRoom == null) return null;

        return ChatRoomResponseDTO.builder()
                .id(chatRoom.getId())
                .name(chatRoom.getName())
                .displayName(chatRoom.getDisplayName())
                .type(chatRoom.getType() != null ? chatRoom.getType().toString() : "PRIVATE")
                .createdBy(chatRoom.getCreatedBy())
                .createdAt(chatRoom.getCreatedAt())
                .updatedAt(chatRoom.getUpdatedAt())
                .participantCount(chatRoom.getParticipants() != null ? chatRoom.getParticipants().size() : 0)
                .unreadCount(unreadCount != null ? unreadCount : 0L)
                .build();
    }

    public static ChatRoomSummaryDTO toChatRoomSummary(ChatRoom chatRoom, Long unreadCount) {
        if (chatRoom == null) return null;

        // Get last message if available
        Message lastMessage = null;
        if (chatRoom.getMessages() != null && !chatRoom.getMessages().isEmpty()) {
            lastMessage = chatRoom.getMessages().stream()
                    .max((m1, m2) -> m1.getSentAt().compareTo(m2.getSentAt()))
                    .orElse(null);
        }

        return ChatRoomSummaryDTO.builder()
                .id(chatRoom.getId())
                .displayName(chatRoom.getDisplayName())
                .type(chatRoom.getType() != null ? chatRoom.getType().toString() : "PRIVATE")
                .participantCount(chatRoom.getParticipants() != null ? chatRoom.getParticipants().size() : 0)
                .lastMessageAt(lastMessage != null ? lastMessage.getSentAt() : null)
                .lastMessagePreview(lastMessage != null ? truncate(lastMessage.getContent(), 50) : null)
                .unreadCount(unreadCount != null ? unreadCount : 0L)
                .isActive(true)
                .build();
    }

    public static ChatRoomListResponseDTO toChatRoomListResponse(Page<ChatRoom> page, Long userId) {
        if (page == null) {
            return ChatRoomListResponseDTO.builder()
                    .chatRooms(Collections.emptyList())
                    .totalElements(0L)
                    .totalPages(0)
                    .currentPage(0)
                    .size(0)
                    .totalUnreadCount(0L)
                    .build();
        }

        List<ChatRoomSummaryDTO> summaries = page.getContent().stream()
                .map(room -> toChatRoomSummary(room, 0L))
                .collect(Collectors.toList());

        return ChatRoomListResponseDTO.builder()
                .chatRooms(summaries)
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .currentPage(page.getNumber())
                .size(page.getSize())
                .totalUnreadCount(0L)
                .build();
    }

    public static ChatRoomListResponseDTO toChatRoomListResponse(List<ChatRoom> rooms) {
        if (rooms == null || rooms.isEmpty()) {
            return ChatRoomListResponseDTO.builder()
                    .chatRooms(Collections.emptyList())
                    .totalElements(0L)
                    .build();
        }

        List<ChatRoomSummaryDTO> summaries = rooms.stream()
                .map(room -> toChatRoomSummary(room, 0L))
                .collect(Collectors.toList());

        return ChatRoomListResponseDTO.builder()
                .chatRooms(summaries)
                .totalElements((long) rooms.size())
                .build();
    }

    // ========== CHAT PARTICIPANT CONVERSIONS ==========

    public static ChatParticipantDTO toParticipantDTO(ChatParticipant participant) {
        if (participant == null) return null;

        return ChatParticipantDTO.builder()
                .id(participant.getId())
                .userId(participant.getUserId())
                .userName(participant.getUserName())
                .userEmail(participant.getUserEmail())
                .role(participant.getRole() != null ? participant.getRole().toString() : "MEMBER")
                .joinedAt(participant.getJoinedAt())
                .isActive(participant.getIsActive())
                .isOnline(false) // Default, can be updated from WebSocket status
                .lastReadAt(participant.getLastReadAt())
                .unreadCount(0L) // Default, can be calculated separately
                .build();
    }

    public static ChatParticipantListResponseDTO toParticipantListResponse(List<ChatParticipant> participants) {
        if (participants == null || participants.isEmpty()) {
            return ChatParticipantListResponseDTO.builder()
                    .participants(Collections.emptyList())
                    .totalParticipants(0)
                    .adminCount(0)
                    .memberCount(0)
                    .onlineCount(0)
                    .activeCount(0)
                    .build();
        }

        List<ChatParticipantDTO> dtos = participants.stream()
                .map(ChatMapper::toParticipantDTO)
                .collect(Collectors.toList());

        long adminCount = participants.stream().filter(p -> p.getRole() == ParticipantRole.ADMIN).count();
        long memberCount = participants.stream().filter(p -> p.getRole() == ParticipantRole.MEMBER).count();
        long activeCount = participants.stream().filter(ChatParticipant::isActiveParticipant).count();

        return ChatParticipantListResponseDTO.builder()
                .participants(dtos)
                .totalParticipants(participants.size())
                .adminCount((int) adminCount)
                .memberCount((int) memberCount)
                .onlineCount(0) // Would need WebSocket connection status
                .activeCount((int) activeCount)
                .build();
    }

    // ========== HELPER METHODS ==========

    private static String truncate(String str, int maxLength) {
        if (str == null) return null;
        if (str.length() <= maxLength) return str;
        return str.substring(0, maxLength - 3) + "...";
    }
}