package com.linkups.api.controller.v1;

import com.linkups.api.dto.request.chat.*;
import com.linkups.api.dto.response.chat.*;
import com.linkups.api.dto.response.common.SuccessResponseDTO;
import com.linkups.api.mapper.ChatMapper;
import com.linkups.api.mapper.MessageMapper;
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

import java.time.LocalDateTime;
import java.util.List;

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
    public ResponseEntity<ChatRoomListResponseDTO> getUserChatRooms(@RequestParam Long userId,
                                                                    @RequestParam(defaultValue = "0") int page,
                                                                    @RequestParam(defaultValue = "10") int size) {
        log.info("Getting chat rooms for user {} (page: {}, size: {})", userId, page, size);

        ChatRoomListResponseDTO response;
        if (size > 0) {
            Page<ChatRoom> chatRoomsPage = chatService.getUserChatRooms(userId, page, size);
            response = ChatMapper.toChatRoomListResponse(chatRoomsPage, userId);
            log.info("Retrieved {} chat rooms (paginated) for user {}", chatRoomsPage.getContent().size(), userId);
        } else {
            List<ChatRoom> chatRooms = chatService.getUserChatRooms(userId);
            response = ChatMapper.toChatRoomListResponse(chatRooms, userId);
            log.info("Retrieved {} chat rooms for user {}", chatRooms.size(), userId);
        }
        return ResponseEntity.ok(response);
    }

    @GetMapping("/rooms/{roomId}")
    public ResponseEntity<ChatRoomResponseDTO> getChatRoom(@PathVariable Long roomId, @RequestParam Long userId) {
        log.info("Getting chat room {} for user {}", roomId, userId);

        ChatRoom chatRoom = chatService.getChatRoom(roomId, userId);
        long unreadCount = messageService.getUnreadMessagesCount(roomId, userId);

        ChatRoomResponseDTO response = ChatMapper.toChatRoomResponse(chatRoom, userId, unreadCount);

        log.info("Retrieved chat room {} details for user {}", roomId, userId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/rooms/group")
    public ResponseEntity<ChatRoomResponseDTO> createGroupChat(@Valid @RequestBody CreateGroupChatRequest request) {
        log.info("Creating group chat '{}' by user {} with {} participants",
                request.getChatName(), request.getCreatorId(), request.getParticipantIds().size());

        ChatRoom chatRoom = chatService.createGroupChat(request.getCreatorId(), request.getChatName(), request.getParticipantIds());
        ChatRoomResponseDTO response = ChatMapper.toChatRoomResponse(chatRoom, request.getCreatorId(), 0L);

        log.info("Group chat '{}' created successfully with ID {}", request.getChatName(), chatRoom.getId());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/rooms/private")
    public ResponseEntity<ChatRoomResponseDTO> getOrCreatePrivateChat(@Valid @RequestBody CreatePrivateChatRequest request) {
        log.info("Getting/creating private chat between users {} and {}", request.getUserId1(), request.getUserId2());

        ChatRoom chatRoom = chatService.getOrCreatePrivateChat(request.getUserId1(), request.getUserId2());
        ChatRoomResponseDTO response = ChatMapper.toChatRoomResponse(chatRoom, request.getUserId1(), 0L);

        log.info("Private chat {} ready for users {} and {}", chatRoom.getId(), request.getUserId1(), request.getUserId2());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/rooms/{roomId}/participants")
    public ResponseEntity<ChatParticipantListResponseDTO> getChatParticipants(@PathVariable Long roomId, @RequestParam Long userId) {
        log.info("Getting participants of room {} for user {}", roomId, userId);

        List<ChatParticipant> participants = chatService.getChatParticipants(roomId, userId);
        ChatParticipantListResponseDTO response = ChatMapper.toParticipantListResponse(participants);

        log.info("Retrieved {} participants for room {}", participants.size(), roomId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/rooms/{roomId}/participants")
    public ResponseEntity<SuccessResponseDTO> addUserToGroup(@PathVariable Long roomId,
                                                              @Valid @RequestBody AddUserToGroupRequest request) {
        log.info("Adding user {} to room {} by user {}", request.getUserId(), roomId, request.getRequestingUserId());

        chatService.addUserToGroupChat(roomId, request.getUserId(), request.getRequestingUserId());

        SuccessResponseDTO response = SuccessResponseDTO.of("User added to group successfully");

        log.info("User {} added to room {} successfully", request.getUserId(), roomId);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/rooms/{roomId}/participants/{userId}")
    public ResponseEntity<SuccessResponseDTO> removeUserFromGroup(@PathVariable Long roomId,
                                                                   @PathVariable Long userId,
                                                                   @RequestParam Long requestingUserId) {
        log.info("Removing user {} from room {} by user {}", userId, roomId, requestingUserId);

        chatService.removeUsersFromChat(roomId, userId, requestingUserId);

        SuccessResponseDTO response = SuccessResponseDTO.of("User removed from group successfully");

        log.info("User {} removed from room {} successfully", userId, roomId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/rooms/{roomId}/messages")
    public ResponseEntity<MessageListResponseDTO> getMessageHistory(@PathVariable Long roomId,
                                                                 @RequestParam Long userId,
                                                                 @RequestParam(defaultValue = "0") int page,
                                                                 @RequestParam(defaultValue = "20") int size) {
        log.info("Getting message history for room {} by user {} (page: {}, size: {})", roomId, userId, page, size);

        Page<Message> messagesPage = messageService.getMessageHistory(roomId, userId, page, size);
        MessageListResponseDTO response = MessageMapper.toMessageListResponse(messagesPage);

        log.info("Retrieved {} messages for room {} (page {})", messagesPage.getContent().size(), roomId, page);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/rooms/{roomId}/messages/recent")
    public ResponseEntity<MessageListResponseDTO> getRecentMessages(@PathVariable Long roomId,
                                                                 @RequestParam Long userId,
                                                                 @RequestParam(defaultValue = "50") int limit) {
        log.info("Getting {} recent messages from room {} for user {}", limit, roomId, userId);

        List<Message> messages = messageService.getRecentMessages(roomId, userId, limit);
        MessageListResponseDTO response = MessageMapper.toMessageListResponse(messages);

        log.info("Retrieved {} recent messages from room {}", messages.size(), roomId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/rooms/{roomId}/messages/after")
    public ResponseEntity<MessageListResponseDTO> getMessagesAfter(@PathVariable Long roomId,
                                                                     @RequestParam Long userId,
                                                                     @RequestParam String afterTime) {
        log.info("Getting messages after {} from room {} for user {}", afterTime, roomId, userId);

        try {
            LocalDateTime afterDateTime = LocalDateTime.parse(afterTime);
            List<Message> messages = messageService.getMessagesAfterTime(userId, roomId, afterDateTime);
            MessageListResponseDTO response = MessageMapper.toMessageListResponse(messages);

            log.info("Retrieved {} messages after {} from room {}", messages.size(), afterTime, roomId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error parsing afterTime parameter: {}", afterTime, e);
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/rooms/{roomId}/messages/unread")
    public ResponseEntity<MessageListResponseDTO> getUnreadMessages(@PathVariable Long roomId, @RequestParam Long userId) {
        log.info("Getting unread messages for user {} in room {}", userId, roomId);

        List<Message> unreadMessages = messageService.getUnreadMessages(roomId, userId);
        long unreadCount = messageService.getUnreadMessagesCount(roomId, userId);

        MessageListResponseDTO response = MessageMapper.toMessageListResponseWithUnread(unreadMessages, unreadCount);

        log.info("Retrieved {} unread messages for user {} in room {}", unreadCount, userId, roomId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/rooms/{roomId}/messages/mark-read")
    public ResponseEntity<SuccessResponseDTO> markMessagesAsRead(@PathVariable Long roomId, @RequestParam Long userId) {
        log.info("Marking messages as read for user {} in room {}", userId, roomId);

        messageService.markMessagesAsRead(roomId, userId);

        SuccessResponseDTO response = SuccessResponseDTO.of("Messages marked as read successfully");

        log.info("Messages marked as read for user {} in room {}", userId, roomId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/messages/search")
    public ResponseEntity<MessageListResponseDTO> searchMessages(@RequestParam Long roomId,
                                                              @RequestParam Long userId,
                                                              @RequestParam String searchTerm) {
        log.info("Searching messages in room {} for user {} with term: '{}'", roomId, userId, searchTerm);

        List<Message> searchResults = messageService.searchMessages(roomId, userId, searchTerm);
        MessageListResponseDTO response = MessageMapper.toMessageListResponse(searchResults);

        log.info("Found {} messages matching '{}' in room {}", searchResults.size(), searchTerm, roomId);
        return ResponseEntity.ok(response);
    }
}