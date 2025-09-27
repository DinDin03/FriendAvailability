package com.linkups.api.controller.v1;

import com.linkups.api.dto.request.chat.*;
import com.linkups.domain.entity.ChatRoom;
import com.linkups.domain.entity.ChatParticipant;
import com.linkups.domain.entity.Message;
import com.linkups.domain.service.ChatService;
import com.linkups.domain.service.MessageService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/chat")
@CrossOrigin(origins = "*")
@Slf4j
public class ChatManagementController {

    private final ChatService chatService;
    private final MessageService messageService;

    public ChatManagementController(ChatService chatService, MessageService messageService) {
        this.chatService = chatService;
        this.messageService = messageService;
        log.info("ChatManagementController initialized successfully");
    }

    @GetMapping("/rooms")
    public ResponseEntity<Map<String, Object>> getUserChatRooms(@RequestParam Long userId,
                                                                @RequestParam(defaultValue = "0") int page,
                                                                @RequestParam(defaultValue = "10") int size) {
        log.info("Getting chat rooms for user {} (page: {}, size: {})", userId, page, size);

        if (size > 0) {
            Page<ChatRoom> chatRoomsPage = chatService.getUserChatRooms(userId, page, size);
            Map<String, Object> response = Map.of(
                    "chatRooms", chatRoomsPage.getContent(),
                    "totalElements", chatRoomsPage.getTotalElements(),
                    "totalPages", chatRoomsPage.getTotalPages(),
                    "currentPage", page,
                    "size", size
            );
            log.info("Retrieved {} chat rooms (paginated) for user {}", chatRoomsPage.getContent().size(), userId);
            return ResponseEntity.ok(response);
        } else {
            List<ChatRoom> chatRooms = chatService.getUserChatRooms(userId);
            Map<String, Object> response = Map.of("chatRooms", chatRooms, "totalElements", chatRooms.size());
            log.info("Retrieved {} chat rooms for user {}", chatRooms.size(), userId);
            return ResponseEntity.ok(response);
        }
    }

    @GetMapping("/rooms/{roomId}")
    public ResponseEntity<Map<String, Object>> getChatRoom(@PathVariable Long roomId, @RequestParam Long userId) {
        log.info("Getting chat room {} for user {}", roomId, userId);

        ChatRoom chatRoom = chatService.getChatRoom(roomId, userId);

        Map<String, Object> response = Map.of(
                "id", chatRoom.getId(),
                "name", chatRoom.getName(),
                "type", chatRoom.getType().toString(),
                "createdBy", chatRoom.getCreatedBy(),
                "createdAt", chatRoom.getCreatedAt(),
                "updatedAt", chatRoom.getUpdatedAt(),
                "isPrivateChat", chatRoom.isPrivateChat(),
                "isGroupChat", chatRoom.isGroupChat(),
                "displayName", chatRoom.getDisplayName()
        );

        log.info("Retrieved chat room {} details for user {}", roomId, userId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/rooms/group")
    public ResponseEntity<Map<String, Object>> createGroupChat(@Valid @RequestBody CreateGroupChatRequest request) {
        log.info("Creating group chat '{}' by user {} with {} participants",
                request.getChatName(), request.getCreatorId(), request.getParticipantIds().size());

        ChatRoom chatRoom = chatService.createGroupChat(request.getCreatorId(), request.getChatName(), request.getParticipantIds());

        Map<String, Object> response = Map.of(
                "message", "Group chat created successfully",
                "chatRoom", Map.of(
                        "id", chatRoom.getId(),
                        "name", chatRoom.getName(),
                        "type", chatRoom.getType().toString(),
                        "createdBy", chatRoom.getCreatedBy(),
                        "createdAt", chatRoom.getCreatedAt()
                )
        );

        log.info("Group chat '{}' created successfully with ID {}", request.getChatName(), chatRoom.getId());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/rooms/private")
    public ResponseEntity<Map<String, Object>> getOrCreatePrivateChat(@Valid @RequestBody CreatePrivateChatRequest request) {
        log.info("Getting/creating private chat between users {} and {}", request.getUserId1(), request.getUserId2());

        ChatRoom chatRoom = chatService.getOrCreatePrivateChat(request.getUserId1(), request.getUserId2());
        boolean isNewChat = chatRoom.getCreatedAt().isAfter(chatRoom.getCreatedAt().minusSeconds(5));

        Map<String, Object> response = Map.of(
                "message", "Private chat ready",
                "chatRoom", Map.of(
                        "id", chatRoom.getId(),
                        "name", chatRoom.getName(),
                        "type", chatRoom.getType().toString(),
                        "createdBy", chatRoom.getCreatedBy(),
                        "createdAt", chatRoom.getCreatedAt()
                ),
                "isNewChat", isNewChat
        );

        log.info("Private chat {} ready for users {} and {}", chatRoom.getId(), request.getUserId1(), request.getUserId2());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/rooms/{roomId}/participants")
    public ResponseEntity<Map<String, Object>> getChatParticipants(@PathVariable Long roomId, @RequestParam Long userId) {
        log.info("Getting participants of room {} for user {}", roomId, userId);

        List<ChatParticipant> participants = chatService.getChatParticipants(roomId, userId);

        Map<String, Object> response = Map.of(
                "participants", participants,
                "totalParticipants", participants.size()
        );

        log.info("Retrieved {} participants for room {}", participants.size(), roomId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/rooms/{roomId}/participants")
    public ResponseEntity<Map<String, String>> addUserToGroup(@PathVariable Long roomId,
                                                              @Valid @RequestBody AddUserToGroupRequest request) {
        log.info("Adding user {} to room {} by user {}", request.getUserId(), roomId, request.getRequestingUserId());

        chatService.addUserToGroupChat(roomId, request.getUserId(), request.getRequestingUserId());

        Map<String, String> response = Map.of("message", "User added to group successfully");

        log.info("User {} added to room {} successfully", request.getUserId(), roomId);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/rooms/{roomId}/participants/{userId}")
    public ResponseEntity<Map<String, String>> removeUserFromGroup(@PathVariable Long roomId,
                                                                   @PathVariable Long userId,
                                                                   @RequestParam Long requestingUserId) {
        log.info("Removing user {} from room {} by user {}", userId, roomId, requestingUserId);

        chatService.removeUsersFromChat(roomId, userId, requestingUserId);

        Map<String, String> response = Map.of("message", "User removed from group successfully");

        log.info("User {} removed from room {} successfully", userId, roomId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/rooms/{roomId}/messages")
    public ResponseEntity<Map<String, Object>> getMessageHistory(@PathVariable Long roomId,
                                                                 @RequestParam Long userId,
                                                                 @RequestParam(defaultValue = "0") int page,
                                                                 @RequestParam(defaultValue = "20") int size) {
        log.info("Getting message history for room {} by user {} (page: {}, size: {})", roomId, userId, page, size);

        Page<Message> messagesPage = messageService.getMessageHistory(roomId, userId, page, size);

        Map<String, Object> response = Map.of(
                "messages", messagesPage.getContent(),
                "totalElements", messagesPage.getTotalElements(),
                "totalPages", messagesPage.getTotalPages(),
                "currentPage", page,
                "size", size
        );

        log.info("Retrieved {} messages for room {} (page {})", messagesPage.getContent().size(), roomId, page);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/rooms/{roomId}/messages/recent")
    public ResponseEntity<Map<String, Object>> getRecentMessages(@PathVariable Long roomId,
                                                                 @RequestParam Long userId,
                                                                 @RequestParam(defaultValue = "50") int limit) {
        log.info("Getting {} recent messages from room {} for user {}", limit, roomId, userId);

        List<Message> messages = messageService.getRecentMessages(roomId, userId, limit);

        Map<String, Object> response = Map.of(
                "messages", messages,
                "totalMessages", messages.size()
        );

        log.info("Retrieved {} recent messages from room {}", messages.size(), roomId);
        return ResponseEntity.ok(response);
    }


    @GetMapping("/rooms/{roomId}/messages/unread")
    public ResponseEntity<Map<String, Object>> getUnreadMessages(@PathVariable Long roomId, @RequestParam Long userId) {
        log.info("Getting unread messages for user {} in room {}", userId, roomId);

        List<Message> unreadMessages = messageService.getUnreadMessages(roomId, userId);
        long unreadCount = messageService.getUnreadMessagesCount(roomId, userId);

        Map<String, Object> response = Map.of(
                "unreadMessages", unreadMessages,
                "unreadCount", unreadCount
        );

        log.info("Retrieved {} unread messages for user {} in room {}", unreadCount, userId, roomId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/rooms/{roomId}/messages/mark-read")
    public ResponseEntity<Map<String, String>> markMessagesAsRead(@PathVariable Long roomId, @RequestParam Long userId) {
        log.info("Marking messages as read for user {} in room {}", userId, roomId);

        messageService.markMessagesAsRead(roomId, userId);

        Map<String, String> response = Map.of("message", "Messages marked as read successfully");

        log.info("Messages marked as read for user {} in room {}", userId, roomId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/messages/search")
    public ResponseEntity<Map<String, Object>> searchMessages(@RequestParam Long roomId,
                                                              @RequestParam Long userId,
                                                              @RequestParam String searchTerm) {
        log.info("Searching messages in room {} for user {} with term: '{}'", roomId, userId, searchTerm);

        List<Message> searchResults = messageService.searchMessages(roomId, userId, searchTerm);

        Map<String, Object> response = Map.of(
                "searchResults", searchResults,
                "totalResults", searchResults.size(),
                "searchTerm", searchTerm
        );

        log.info("Found {} messages matching '{}' in room {}", searchResults.size(), searchTerm, roomId);
        return ResponseEntity.ok(response);
    }
}