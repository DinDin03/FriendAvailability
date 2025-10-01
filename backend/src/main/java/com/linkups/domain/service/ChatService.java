package com.linkups.domain.service;

import com.linkups.domain.entity.*;
import com.linkups.domain.entity.enums.ChatType;
import com.linkups.domain.entity.enums.MessageType;
import com.linkups.domain.entity.enums.ParticipantRole;
import com.linkups.domain.exception.*;
import com.linkups.domain.repository.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
@Slf4j
public class ChatService {

    private final ChatRoomRepository chatRoomRepository;
    private final MessageRepository messageRepository;
    private final ChatParticipantRepository chatParticipantRepository;
    private final FriendRepository friendRepository;
    private final UserService userService;

    public ChatService(ChatRoomRepository chatRoomRepository,
                       MessageRepository messageRepository,
                       ChatParticipantRepository chatParticipantRepository,
                       FriendRepository friendRepository,
                       UserService userService) {
        this.chatParticipantRepository = chatParticipantRepository;
        this.messageRepository = messageRepository;
        this.chatRoomRepository = chatRoomRepository;
        this.friendRepository = friendRepository;
        this.userService = userService;
        log.info("ChatService initialized successfully");
    }

    public Page<ChatRoom> getUserChatRooms(Long userId, int page, int size) {
        log.debug("Getting paginated chat rooms for user: {} (page: {}, size: {})", userId, page, size);

        User user = userService.findUserById(userId); // Validates user exists

        Pageable pageable = PageRequest.of(page, size);
        Page<ChatRoom> chatRooms = chatRoomRepository.findChatRoomsForUser(userId, pageable);

        log.debug("Found {} chat rooms for user: {}", chatRooms.getTotalElements(), userId);
        return chatRooms;
    }

    public List<ChatRoom> getUserChatRooms(Long userId) {
        log.debug("Getting all chat rooms for user: {}", userId);

        User user = userService.findUserById(userId); // Validates user exists

        List<ChatRoom> chatRooms = chatRoomRepository.findChatRoomsForUser(userId);

        log.debug("Found {} chat rooms for user: {}", chatRooms.size(), userId);
        return chatRooms;
    }

    public ChatRoom getChatRoom(Long roomId, Long userId) {
        log.debug("Getting chat room {} for user {}", roomId, userId);

        ChatRoom chatRoom = findChatRoomById(roomId);
        validateUserCanAccessRoom(userId, roomId);

        log.debug("Chat room {} accessed successfully by user {}", roomId, userId);
        return chatRoom;
    }

    public List<ChatParticipant> getChatParticipants(Long roomId, Long userId) {
        log.debug("Getting participants for chat room {} requested by user {}", roomId, userId);

        ChatRoom chatRoom = findChatRoomById(roomId);
        validateUserCanAccessRoom(userId, roomId);

        List<ChatParticipant> participants = chatParticipantRepository.findByChatRoomIdAndIsActiveTrue(roomId);

        log.debug("Retrieved {} participants for chat room {}", participants.size(), roomId);
        return participants;
    }

    public ChatRoom createGroupChat(Long creatorId, String chatName, List<Long> participantIds) {
        log.info("Creating group chat '{}' by user {} with {} participants", chatName, creatorId, participantIds.size());

        validateGroupChatInput(creatorId, chatName, participantIds);

        // Create the chat room
        ChatRoom chatRoom = ChatRoom.builder()
                .name(chatName.trim())
                .type(ChatType.GROUP)
                .createdBy(creatorId)
                .build();

        ChatRoom savedChatRoom = chatRoomRepository.save(chatRoom);

        // Add creator as owner
        addUserToGroupChatInternal(savedChatRoom.getId(), creatorId, ParticipantRole.OWNER);

        // Add other participants as members
        for (Long participantId : participantIds) {
            if (!participantId.equals(creatorId)) {
                addUserToGroupChatInternal(savedChatRoom.getId(), participantId, ParticipantRole.MEMBER);
            }
        }

        createSystemMessage(savedChatRoom.getId(), "Group chat '" + chatName + "' was created");

        log.info("Group chat '{}' created successfully with ID {}", chatName, savedChatRoom.getId());
        return savedChatRoom;
    }

    public ChatRoom getOrCreatePrivateChat(Long userId1, Long userId2) {
        log.debug("Getting or creating private chat between users {} and {}", userId1, userId2);

        if (userId1.equals(userId2)) {
            throw InvalidOperationException.cannotCreatePrivateRoomWithSelf();
        }

        // Validate both users exist
        User user1 = userService.findUserById(userId1);
        User user2 = userService.findUserById(userId2);

        // Check if private room already exists
        Optional<ChatRoom> existingRoom = chatRoomRepository.findPrivateChatBetweenUsers(userId1, userId2);

        if (existingRoom.isPresent()) {
            log.debug("Private chat already exists between users {} and {}", userId1, userId2);
            return existingRoom.get();
        }

        // Create new private chat
        ChatRoom chatRoom = ChatRoom.builder()
                .name("Private Chat")
                .type(ChatType.PRIVATE)
                .createdBy(userId1)
                .build();

        ChatRoom savedChatRoom = chatRoomRepository.save(chatRoom);

        // Add both users as participants
        addUserToPrivateChat(savedChatRoom.getId(), userId1);
        addUserToPrivateChat(savedChatRoom.getId(), userId2);

        log.info("Private chat created successfully between users {} and {} with ID {}",
                userId1, userId2, savedChatRoom.getId());
        return savedChatRoom;
    }

    public void addUserToGroupChat(Long roomId, Long userIdToAdd, Long requestingUserId) {
        log.info("Adding user {} to group chat {} by user {}", userIdToAdd, roomId, requestingUserId);

        ChatRoom chatRoom = findChatRoomById(roomId);
        validateUserIsGroupAdmin(requestingUserId, roomId);
        User userToAdd = userService.findUserById(userIdToAdd);

        if (requestingUserId.equals(userIdToAdd)) {
            throw InvalidOperationException.cannotAddSelfToGroup();
        }

        if (chatParticipantRepository.userExistsInRoom(userIdToAdd, roomId)) {
            throw InvalidOperationException.duplicateParticipantInGroup(userIdToAdd);
        }

        addUserToGroupChatInternal(roomId, userIdToAdd, ParticipantRole.MEMBER);
        createSystemMessage(roomId, userToAdd.getName() + " was added to the group");

        log.info("User {} added to group chat {} successfully", userIdToAdd, roomId);
    }

    public void removeUsersFromChat(Long roomId, Long userIdToRemove, Long requestingUserId) {
        log.info("Removing user {} from chat room {} by user {}", userIdToRemove, roomId, requestingUserId);

        ChatRoom chatRoom = findChatRoomById(roomId);
        validateUserCanAccessRoom(requestingUserId, roomId);
        User userToRemove = userService.findUserById(userIdToRemove);

        // Users can remove themselves, otherwise admin permission required
        if (!requestingUserId.equals(userIdToRemove)) {
            validateUserIsGroupAdmin(requestingUserId, roomId);
        }

        ChatParticipant participant = findActiveParticipant(userIdToRemove, roomId);
        participant.setIsActive(false);
        chatParticipantRepository.save(participant);

        String message = requestingUserId.equals(userIdToRemove) ?
                userToRemove.getName() + " left the group" :
                userToRemove.getName() + " was removed from the group";
        createSystemMessage(roomId, message);

        log.info("User {} removed from chat room {} successfully", userIdToRemove, roomId);
    }

    public void promoteUserToAdmin(Long roomId, Long userIdToPromote, Long requestingUserId) {
        log.info("Promoting user {} to admin in chat room {} by user {}", userIdToPromote, roomId, requestingUserId);

        ChatRoom chatRoom = findChatRoomById(roomId);
        validateUserIsGroupOwner(requestingUserId, roomId);
        User userToPromote = userService.findUserById(userIdToPromote);

        if (requestingUserId.equals(userIdToPromote)) {
            throw InvalidOperationException.cannotPromoteSelfInGroup();
        }

        ChatParticipant participant = findActiveParticipant(userIdToPromote, roomId);
        participant.setRole(ParticipantRole.ADMIN);
        chatParticipantRepository.save(participant);

        createSystemMessage(roomId, userToPromote.getName() + " was promoted to admin");

        log.info("User {} promoted to admin in chat room {} successfully", userIdToPromote, roomId);
    }

    public List<ChatRoom> searchGroupChats(String searchTerm) {
        log.debug("Searching group chats with term: {}", searchTerm);

        if (searchTerm == null || searchTerm.trim().length() < 2) {
            throw ValidationException.invalidMessageSearchTerm();
        }

        List<ChatRoom> results = chatRoomRepository.findGroupChatsByNameContaining(searchTerm.trim());

        log.debug("Found {} group chats matching search term: {}", results.size(), searchTerm);
        return results;
    }

    private void validateGroupChatInput(Long creatorId, String chatName, List<Long> participantIds) {
        User creator = userService.findUserById(creatorId);

        if (chatName == null || chatName.trim().isEmpty()) {
            throw ValidationException.chatRoomNameRequired();
        }

        if (chatName.trim().length() < 2) {
            throw ValidationException.chatRoomNameTooShort(2);
        }

        if (chatName.trim().length() > 100) {
            throw ValidationException.chatRoomNameTooLong(100);
        }

        if (participantIds == null || participantIds.isEmpty()) {
            throw InvalidOperationException.groupChatTooFewParticipants();
        }

        // Validate all participant users exist
        for (Long participantId : participantIds) {
            userService.findUserById(participantId);
        }
    }

    private ChatRoom findChatRoomById(Long roomId) {
        return chatRoomRepository.findById(roomId)
                .orElseThrow(() -> {
                    log.warn("Chat room not found with ID: {}", roomId);
                    return ResourceNotFoundException.chatRoomNotFound(roomId);
                });
    }

    private void validateUserCanAccessRoom(Long userId, Long roomId) {
        if (!chatParticipantRepository.userExistsInRoom(userId, roomId)) {
            log.warn("User {} attempted to access chat room {} without permission", userId, roomId);
            throw InsufficientPermissionException.cannotAccessChatRoom(roomId);
        }
    }

    private void validateUserIsGroupAdmin(Long userId, Long roomId) {
        ChatParticipant participant = findActiveParticipant(userId, roomId);
        if (!participant.isAdmin()) {
            log.warn("User {} attempted admin operation in chat room {} without admin permissions", userId, roomId);
            throw InsufficientPermissionException.cannotAddUsersToGroup();
        }
    }

    private void validateUserIsGroupOwner(Long userId, Long roomId) {
        ChatParticipant participant = findActiveParticipant(userId, roomId);
        if (!participant.getRole().equals(ParticipantRole.OWNER)) {
            log.warn("User {} attempted owner operation in chat room {} without owner permissions", userId, roomId);
            throw InsufficientPermissionException.cannotPromoteUser();
        }
    }

    private ChatParticipant findActiveParticipant(Long userId, Long roomId) {
        return chatParticipantRepository.findActiveParticipation(userId, roomId)
                .orElseThrow(() -> {
                    log.warn("Active participant not found for user {} in room {}", userId, roomId);
                    return ResourceNotFoundException.chatParticipantNotFound(userId, roomId);
                });
    }

    private void addUserToPrivateChat(Long roomId, Long userId) {
        User user = userService.findUserById(userId);
        ChatRoom chatRoom = findChatRoomById(roomId);

        ChatParticipant participant = ChatParticipant.builder()
                .user(user)
                .chatRoom(chatRoom)
                .userId(userId)
                .chatRoomId(roomId)
                .role(ParticipantRole.MEMBER)
                .build();
        chatParticipantRepository.save(participant);
    }

    private void addUserToGroupChatInternal(Long roomId, Long userId, ParticipantRole role) {
        if (chatParticipantRepository.userExistsInRoom(userId, roomId)) {
            chatParticipantRepository.reactivateUserInRoom(userId, roomId);
        } else {
            User user = userService.findUserById(userId);
            ChatRoom chatRoom = findChatRoomById(roomId);

            ChatParticipant participant = ChatParticipant.builder()
                    .user(user)
                    .chatRoom(chatRoom)
                    .userId(userId)
                    .chatRoomId(roomId)
                    .role(role)
                    .build();
            chatParticipantRepository.save(participant);
        }
    }

    private void createSystemMessage(Long roomId, String content) {
        Message systMessage = Message.builder()
                .chatRoomId(roomId)
                .content(content)
                .messageType(MessageType.SYSTEM_MESSAGE)
                .senderId(null)
                .build();
        messageRepository.save(systMessage);
    }
}