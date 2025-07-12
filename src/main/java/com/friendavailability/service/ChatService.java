package com.friendavailability.service;

import com.friendavailability.model.*;
import com.friendavailability.repository.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import javax.management.RuntimeErrorException;

@Service
@Transactional
public class ChatService {

    private final ChatRoomRepository chatRoomRepository;
    private final MessageRepository messageRepository;
    private final ChatParticipantRepository chatParticipantRepository;
    private final FriendRepository friendRepository;
    private final UserRepository userRepository;

    public ChatService(ChatRoomRepository chatRoomRepository,
            MessageRepository messageRepository, ChatParticipantRepository chatParticipantRepository,
            FriendRepository friendRepository, UserRepository userRepository) {
        this.chatParticipantRepository = chatParticipantRepository;
        this.messageRepository = messageRepository;
        this.chatRoomRepository = chatRoomRepository;
        this.friendRepository = friendRepository;
        this.userRepository = userRepository;
    }

    public Page<ChatRoom> getUserChatRooms(Long userId, int page, int size) {
        validateUserExists(userId);

        Pageable pageable = PageRequest.of(page, size);
        Page<ChatRoom> chatRooms = chatRoomRepository.findChatRoomsForUser(userId, pageable);

        return chatRooms;
    }

    public List<ChatRoom> getUserChatRooms(Long userId) {
        validateUserExists(userId);
        return chatRoomRepository.findChatRoomsForUser(userId);
    }

    public ChatRoom getChatRoom(Long roomId, Long userId) {
        ChatRoom chatRoom = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new RuntimeException("Chatroom not found with id: " + roomId));
        if (!chatParticipantRepository.isUserActiveInRoom(userId, roomId)) {
            throw new RuntimeException("User is not authorised to access this chat room");
        }
        return chatRoom;

    }

    public ChatRoom getOrCreatePrivateChat(Long userId1, Long userId2) {
        if (userId1.equals(userId2)) {
            throw new RuntimeException("Cannot create a room with yourself");
        }

        validateUserExists(userId1);
        validateUserExists(userId2);

        validateUsersAreFriends(userId1, userId2);

        Optional<ChatRoom> existingChat = chatRoomRepository.findPrivateChatBetweenUsers(userId1, userId2);

        if (existingChat.isPresent()) {
            return existingChat.get();
        }

        return createNewPrivateChat(userId1, userId2);

    }

    public ChatRoom createNewPrivateChat(Long userId1, Long userId2) {
        try {
            ChatRoom chatRoom = ChatRoom.builder()
                    .type(ChatType.PRIVATE)
                    .createdBy(userId1)
                    .name(null)
                    .build();

            ChatRoom savedRoom = chatRoomRepository.save(chatRoom);

            addUserToPrivateChat(savedRoom.getId(), userId1, ParticipantRole.ADMIN);
            addUserToPrivateChat(savedRoom.getId(), userId2, ParticipantRole.MEMBER);

            createSystemMessage(savedRoom.getId(), "Private chat started");

            return savedRoom;
        } catch (Exception e) {
            throw new RuntimeException("Failed to create private chat " + e.getMessage());
        }
    }

    public ChatRoom createGroupChat(Long creatorId, String chatName, List<Long> participantIds) {
        validateUserExists(creatorId);

        if (chatName == null || chatName.trim().isEmpty()) {
            throw new RuntimeException("Chat name is required");
        }

        if (participantIds == null || participantIds.isEmpty()) {
            throw new RuntimeException("At least one pariticapnt is required for a group chat");
        }

        for (Long participantId : participantIds) {
            validateUserExists(participantId);
            if (!participantId.equals(creatorId)) {
                validateUsersAreFriends(creatorId, participantId);
            }
        }

        try {
            ChatRoom chatRoom = ChatRoom.builder()
                    .type(ChatType.GROUP)
                    .name(chatName.trim())
                    .createdBy(creatorId)
                    .build();

            ChatRoom savedRoom = chatRoomRepository.save(chatRoom);

            addUserToGroupChatInternal(savedRoom.getId(), creatorId, ParticipantRole.ADMIN);

            for (Long participantId : participantIds) {
                if (!participantId.equals(creatorId)) {
                    addUserToGroupChatInternal(savedRoom.getId(), participantId, ParticipantRole.MEMBER);
                }
            }

            createSystemMessage(savedRoom.getId(), "Group chat " + chatName + " created");

            return savedRoom;
        } catch (Exception e) {
            throw new RuntimeException("Failed to create group chat " + e.getMessage());
        }
    }

    public void addUserToGroupChat(Long roomId, Long userId, Long requestingUserId) {
        if (!chatParticipantRepository.isUserAdminInRoom(requestingUserId, roomId)) {
            throw new RuntimeException("Only admins can add users to the group");
        }
        validateUserExists(userId);

        if (chatParticipantRepository.isUserActiveInRoom(userId, roomId)) {
            throw new RuntimeException("User already exists in the chat room");
        }

        validateUsersAreFriends(requestingUserId, userId);

        addUserToGroupChatInternal(roomId, userId, ParticipantRole.MEMBER);

        User addedUser = getUserById(userId);
        createSystemMessage(roomId, addedUser.getName() + " was added to the group chat");

    }

    private void validateUserExists(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new RuntimeException("User not found with ID: " + userId);
        }
    }

    private void validateUsersAreFriends(Long user1Id, Long user2Id) {
        boolean areFriends = friendRepository.existsFriendshipBetweenUsers(user1Id, user2Id);
        if (!areFriends) {
            throw new RuntimeException("Users must be friends to start a chat");
        }
    }

    public List<ChatParticipant> getChatParticipants(Long roomId, Long requestingId) {
        if (!chatParticipantRepository.isUserActiveInRoom(requestingId, roomId)) {
            throw new RuntimeException("User not authorised to view pariticapnts");
        }

        return chatParticipantRepository.findByChatRoomIdAndIsActiveTrue(roomId);
    }

    public void removeUsersFromChat(Long roomId, Long userId, Long requestingUserId){
        boolean isSelfRemoval = userId.equals(requestingUserId);
        boolean isAdmin = chatParticipantRepository.isUserAdminInRoom(requestingUserId, roomId);

        if(!isSelfRemoval && !isAdmin){
            throw new RuntimeException("Only admin or user can remove from the group chat");
        }

        int updated = chatParticipantRepository.removeUserFromRoom(userId, roomId);

        if(updated == 0){
            throw new IllegalArgumentException("User not found in the chat");
        }

        User removedUser = getUserById(userId);
        String message = isSelfRemoval ? removedUser.getName() + " left the chat " : 
        
    } 
}