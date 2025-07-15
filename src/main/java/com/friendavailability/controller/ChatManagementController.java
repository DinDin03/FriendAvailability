package com.friendavailability.controller;

import com.friendavailability.dto.chat.*;
import com.friendavailability.model.ChatRoom;
import com.friendavailability.model.ChatParticipant;
import com.friendavailability.model.Message;
import com.friendavailability.service.ChatService;
import com.friendavailability.service.MessageService;
import com.friendavailability.service.UserService;

import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/chat")
@CrossOrigin(origins = "*")
public class ChatManagementController {

    private final ChatService chatService;
    private final MessageService messageService;
    private final UserService userService;

    public ChatManagementController(ChatService chatService,
            MessageService messageService,
            UserService userService) {
        this.chatService = chatService;
        this.messageService = messageService;
        this.userService = userService;
    }

    @GetMapping("/rooms")
    public ResponseEntity<?> getUserChatRooms(@RequestParam Long userId, @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        try {
            if (size > 0) {
                Page<ChatRoom> chatRoomsPage = chatService.getUserChatRooms(userId, page, size);
                return ResponseEntity.ok(Map.of(
                        "chatRooms", chatRoomsPage.getContent(),
                        "totalElements", chatRoomsPage.getTotalElements(),
                        "totalPages", chatRoomsPage.getTotalPages(),
                        "currentPage", page,
                        "size", size));
            } else {
                List<ChatRoom> chatRooms = chatService.getUserChatRooms(userId);
                return ResponseEntity.ok(Map.of(
                        "chatRooms", chatRooms,
                        "totalElements", chatRooms.size()));
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", e.getMessage(),
                    "errorCode", "GET_ROOMS_FAILED"));
        }
    }

    @GetMapping("/rooms/{roomId}")
    public ResponseEntity<?> getChatRoom(@PathVariable Long roomId, @RequestParam Long userId) {
        try {
            ChatRoom chatRoom = chatService.getChatRoom(roomId, userId);

            return ResponseEntity.ok(Map.of(
                    "id", chatRoom.getId(),
                    "name", chatRoom.getName(),
                    "type", chatRoom.getType().toString(),
                    "createdBy", chatRoom.getCreatedBy(),
                    "createdAt", chatRoom.getCreatedAt(),
                    "updatedAt", chatRoom.getUpdatedAt(),
                    "isPrivateChat", chatRoom.isPrivateChat(),
                    "isGroupChat", chatRoom.isGroupChat(),
                    "displayName", chatRoom.getDisplayName()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", e.getMessage(),
                    "errorCode", "GET_ROOM_FAILED"));
        }
    }

    @PostMapping("/rooms/group")
    public ResponseEntity<?> createGroupChat(@RequestBody CreateGroupChatRequest request) {
        try {
            ChatRoom chatRoom = chatService.createGroupChat(request.getCreatorId(), request.getChatName(),
                    request.getParticipantIds());

            return ResponseEntity.ok(Map.of(
                    "message", "Group chat created successfully",
                    "chatRoom", Map.of(
                            "id", chatRoom.getId(),
                            "name", chatRoom.getName(),
                            "type", chatRoom.getType().toString(),
                            "createdBy", chatRoom.getCreatedBy(),
                            "createdAt", chatRoom.getCreatedAt())));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", e.getMessage(),
                    "errorCode", "CREATE_GROUP_FAILED"));
        }
    }

    @PostMapping("/rooms/private")
    public ResponseEntity<?> getOrCreatePrivateChat(@RequestBody CreatePrivateChatRequest request) {
        try {
            ChatRoom chatRoom = chatService.getOrCreatePrivateChat(request.getUserId1(), request.getUserId2());

            return ResponseEntity.ok(Map.of(
                    "message", "Private chat ready",
                    "chatRoom", Map.of(
                            "id", chatRoom.getId(),
                            "name", chatRoom.getName(),
                            "type", chatRoom.getType().toString(),
                            "createdBy", chatRoom.getCreatedBy(),
                            "createdAt", chatRoom.getCreatedAt()),
                    "isNewChat", chatRoom.getCreatedAt().isAfter(
                            chatRoom.getCreatedAt().minusSeconds(5))));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", e.getMessage(),
                    "errorCode", "CREATE_PRIVATE_FAILED"));
        }
    }

    @GetMapping("/rooms/{roomId}/participants")
    public ResponseEntity<?> getChatParticipants(@PathVariable Long roomId, @RequestParam Long userId) {
        try {
            List<ChatParticipant> participants = chatService.getChatParticipants(roomId, userId);
            return ResponseEntity.ok(Map.of(
                    "participants", participants,
                    "totalParticipants", participants.size()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", e.getMessage(),
                    "errorCode", "GET_PARTICIPANTS_FAILED"));
        }
    }

    @PostMapping("/rooms/{roomId}/participants")
    public ResponseEntity<?> addUserToGroup(@PathVariable Long roomId, @RequestBody AddUserToGroupRequest request) {
        try {
            chatService.addUserToGroupChat(roomId, request.getUserId(), request.getRequestingUserId());
            return ResponseEntity.ok(Map.of(
                    "message", "User added to group successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", e.getMessage(),
                    "errorCode", "ADD_USER_FAILED"));
        }
    }

    @DeleteMapping("/rooms/{roomId}/participants/{userId}")
    public ResponseEntity<?> removeUserFromGroup(@PathVariable Long roomId, @PathVariable Long userId,
            @RequestParam Long requestingUserId) {
        try {
            chatService.removeUsersFromChat(roomId, userId, requestingUserId);
            return ResponseEntity.ok(Map.of(
                    "message", "User removed from group successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", e.getMessage(),
                    "errorCode", "REMOVE_USER_FAILED"));
        }
    }

    @GetMapping("/rooms/{roomId}/messages")
    public ResponseEntity<?> getMessageHistory(@PathVariable Long roomId, @RequestParam Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        try{
            Page<Message> messagesPage = messageService.getMessageHistory(roomId, userId, page, size);
            return ResponseEntity.ok(Map.of(
                "messages", messagesPage.getContent(),
                "totalElements", messagesPage.getTotalElements(),
                "totalPages", messagesPage.getTotalPages(),
                "currentPage", page,
                "size", size
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", e.getMessage(),
                "errorCode", "GET_MESSAGES_FAILED"
            ));
        }
    }

    @GetMapping("/rooms/{roomId}/messages/recent")
    public ResponseEntity<?> getRecentMessages(@PathVariable Long roomId,
                                             @RequestParam Long userId,
                                             @RequestParam(defaultValue = "50") int limit) {
        try {
            List<Message> messages = messageService.getRecentMessages(roomId, userId, limit);
            
            return ResponseEntity.ok(Map.of(
                "messages", messages,
                "totalMessages", messages.size()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", e.getMessage(),
                "errorCode", "GET_RECENT_MESSAGES_FAILED"
            ));
        }
    }

    @GetMapping("/rooms/{roomId}/messages/unread")
    public ResponseEntity<?> getUnreadMessages(@PathVariable Long roomId,
                                             @RequestParam Long userId) {
        try {
            List<Message> unreadMessages = messageService.getUnreadMessages(roomId, userId);
            long unreadCount = messageService.getUnreadMessagesCount(roomId, userId);
            
            return ResponseEntity.ok(Map.of(
                "unreadMessages", unreadMessages,
                "unreadCount", unreadCount
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", e.getMessage(),
                "errorCode", "GET_UNREAD_FAILED"
            ));
        }
    }

    @PostMapping("/rooms/{roomId}/messages/mark-read")
    public ResponseEntity<?> markMessagesAsRead(@PathVariable Long roomId,
                                              @RequestParam Long userId) {
        try {
            messageService.markMessagesAsRead(roomId, userId);
            
            return ResponseEntity.ok(Map.of(
                "message", "Messages marked as read successfully"
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", e.getMessage(),
                "errorCode", "MARK_READ_FAILED"
            ));
        }
    }

    @GetMapping("/messages/search")
    public ResponseEntity<?> searchMessages(@RequestParam Long roomId,
                                          @RequestParam Long userId,
                                          @RequestParam String searchTerm) {
        try {
            List<Message> searchResults = messageService.searchMessages(roomId, userId, searchTerm);
            
            return ResponseEntity.ok(Map.of(
                "searchResults", searchResults,
                "totalResults", searchResults.size(),
                "searchTerm", searchTerm
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", e.getMessage(),
                "errorCode", "SEARCH_FAILED"
            ));
        }
    }



}
