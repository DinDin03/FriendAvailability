package com.linkups.domain.service;

import com.linkups.domain.entity.*;
import com.linkups.domain.entity.enums.MessageType;
import com.linkups.domain.exception.InsufficientPermissionException;
import com.linkups.domain.exception.ResourceNotFoundException;
import com.linkups.domain.exception.ValidationException;
import com.linkups.domain.exception.InvalidOperationException;
import com.linkups.domain.repository.*;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Service for handling chat message operations with enterprise-grade exception handling
 *
 * This service follows the clean architecture pattern established in LINK-34:
 * - Throws specific custom exceptions instead of RuntimeException
 * - Comprehensive logging for debugging and monitoring
 * - Proper business logic validation and error handling
 * - Clean separation of concerns for message operations
 */
@Service
@Transactional
@Slf4j
public class MessageService {

    private final MessageRepository messageRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final ChatParticipantRepository chatParticipantRepository;
    private final UserRepository userRepository;

    public MessageService(MessageRepository messageRepository,
                          ChatRoomRepository chatRoomRepository,
                          ChatParticipantRepository chatParticipantRepository,
                          UserRepository userRepository) {
        this.messageRepository = messageRepository;
        this.chatParticipantRepository = chatParticipantRepository;
        this.chatRoomRepository = chatRoomRepository;
        this.userRepository = userRepository;
        log.info("MessageService initialised successfully");
    }

    /**
     * Sends a message to a chat room
     *
     * @param senderId ID of the user sending the message
     * @param roomId ID of the chat room
     * @param content Message content
     * @return The saved message entity
     * @throws ValidationException if content is invalid
     * @throws ResourceNotFoundException if user or room not found
     * @throws InsufficientPermissionException if user cannot access room
     */
    public Message sendMessage(Long senderId, Long roomId, String content) {
        log.debug("Sending message from user {} to room {}", senderId, roomId);

        validateUserCanAccessRoom(senderId, roomId);
        validateMessageContent(content);

        // Fetch User and ChatRoom entities to ensure proper relationships
        User sender = userRepository.findById(senderId)
                .orElseThrow(() -> ResourceNotFoundException.userNotFound(senderId));
        ChatRoom chatRoom = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> ResourceNotFoundException.chatRoomNotFound(roomId));

        Message message = Message.builder()
                .sender(sender)
                .chatRoom(chatRoom)
                .senderId(senderId)
                .chatRoomId(roomId)
                .content(content.trim())
                .messageType(MessageType.TEXT)
                .sentAt(LocalDateTime.now())
                .build();

        Message savedMessage = messageRepository.save(message);
        updateRoomLastActivity(roomId);

        log.info("Message sent successfully from user {} to room {}", senderId, roomId);
        return savedMessage;
    }

    /**
     * Gets paginated message history for a room
     *
     * @param roomId ID of the chat room
     * @param userId ID of the requesting user
     * @param page Page number (0-based)
     * @param size Number of messages per page
     * @return Page of messages
     * @throws ResourceNotFoundException if user or room not found
     * @throws InsufficientPermissionException if user cannot access room
     */
    public Page<Message> getMessageHistory(Long roomId, Long userId, int page, int size) {
        log.debug("Getting message history for room {} by user {} (page: {}, size: {})", roomId, userId, page, size);

        validateUserCanAccessRoom(userId, roomId);
        Pageable pageable = PageRequest.of(page, size);
        Page<Message> messagePage = messageRepository.findByChatRoomIdOrderBySentAtDesc(roomId, pageable);

        log.debug("Retrieved {} messages for room {}", messagePage.getContent().size(), roomId);
        return messagePage;
    }

    /**
     * Gets recent messages from a room
     *
     * @param roomId ID of the chat room
     * @param userId ID of the requesting user
     * @param limit Maximum number of messages to return
     * @return List of recent messages
     * @throws ResourceNotFoundException if user or room not found
     * @throws InsufficientPermissionException if user cannot access room
     */
    public List<Message> getRecentMessages(Long roomId, Long userId, int limit) {
        log.debug("Getting {} recent messages from room {} for user {}", limit, roomId, userId);

        validateUserCanAccessRoom(userId, roomId);
        List<Message> recentMessages = messageRepository.findRecentMessagesInRoom(roomId, limit);

        log.debug("Retrieved {} recent messages from room {}", recentMessages.size(), roomId);
        return recentMessages;
    }

    /**
     * Gets messages sent after a specific time
     *
     * @param userId ID of the requesting user
     * @param roomId ID of the chat room
     * @param afterTime Only get messages sent after this time
     * @return List of messages sent after the specified time
     * @throws ResourceNotFoundException if user or room not found
     * @throws InsufficientPermissionException if user cannot access room
     */
    public List<Message> getMessagesAfterTime(Long userId, Long roomId, LocalDateTime afterTime) {
        log.debug("Getting messages after {} from room {} for user {}", afterTime, roomId, userId);

        validateUserCanAccessRoom(userId, roomId);
        List<Message> newMessages = messageRepository.findMessagesAfterTime(roomId, afterTime);

        log.debug("Retrieved {} messages after {} from room {}", newMessages.size(), afterTime, roomId);
        return newMessages;
    }

    /**
     * Gets count of unread messages for a user in a room
     *
     * @param roomId ID of the chat room
     * @param userId ID of the user
     * @return Number of unread messages
     * @throws ResourceNotFoundException if user or room not found
     * @throws InsufficientPermissionException if user cannot access room
     */
    public long getUnreadMessagesCount(Long roomId, Long userId) {
        log.debug("Getting unread message count for user {} in room {}", userId, roomId);

        validateUserCanAccessRoom(userId, roomId);
        LocalDateTime lastReadTime = getUserLastReadTime(userId, roomId);
        long unreadCount = messageRepository.countUnreadMessages(roomId, userId, lastReadTime);

        log.debug("User {} has {} unread messages in room {}", userId, unreadCount, roomId);
        return unreadCount;
    }

    /**
     * Gets unread messages for a user in a room
     *
     * @param roomId ID of the chat room
     * @param userId ID of the user
     * @return List of unread messages
     * @throws ResourceNotFoundException if user or room not found
     * @throws InsufficientPermissionException if user cannot access room
     */
    public List<Message> getUnreadMessages(Long roomId, Long userId) {
        log.debug("Getting unread messages for user {} in room {}", userId, roomId);

        validateUserCanAccessRoom(userId, roomId);
        LocalDateTime lastReadTime = getUserLastReadTime(userId, roomId);
        List<Message> unreadMessages = messageRepository.findUnreadMessages(roomId, userId, lastReadTime);

        log.debug("Retrieved {} unread messages for user {} in room {}", unreadMessages.size(), userId, roomId);
        return unreadMessages;
    }

    /**
     * Marks all messages in a room as read for a user
     *
     * @param roomId ID of the chat room
     * @param userId ID of the user
     * @throws ResourceNotFoundException if user or room not found
     * @throws InsufficientPermissionException if user cannot access room
     * @throws InvalidOperationException if marking as read fails
     */
    public void markMessagesAsRead(Long roomId, Long userId) {
        log.debug("Marking messages as read for user {} in room {}", userId, roomId);

        validateUserCanAccessRoom(userId, roomId);

        try {
            LocalDateTime now = LocalDateTime.now();
            chatParticipantRepository.updateLastReadTime(userId, roomId, now);
            log.info("Messages marked as read for user {} in room {}", userId, roomId);
        } catch (Exception e) {
            log.error("Failed to mark messages as read for user {} in room {}: {}", userId, roomId, e.getMessage());
            throw InvalidOperationException.failedToUpdateReadStatus(userId, roomId);
        }
    }

    /**
     * Gets total count of unread messages across all rooms for a user
     *
     * @param userId ID of the user
     * @return Total number of unread messages
     * @throws ResourceNotFoundException if user not found
     */
    public long getTotalUnreadCount(Long userId) {
        log.debug("Getting total unread count for user {}", userId);

        if (!userRepository.existsById(userId)) {
            log.warn("Attempted to get unread count for non-existent user: {}", userId);
            throw ResourceNotFoundException.userNotFound(userId);
        }

        long unreadCount = messageRepository.countTotalUnreadMessagesForUser(userId);
        log.debug("User {} has {} total unread messages", userId, unreadCount);
        return unreadCount;
    }

    /**
     * Searches for messages in a room containing the search term
     *
     * @param roomId ID of the chat room
     * @param userId ID of the requesting user
     * @param searchTerm Term to search for
     * @return List of matching messages
     * @throws ResourceNotFoundException if user or room not found
     * @throws InsufficientPermissionException if user cannot access room
     * @throws ValidationException if search term is invalid
     */
    public List<Message> searchMessages(Long roomId, Long userId, String searchTerm) {
        log.debug("Searching messages in room {} for user {} with term: {}", roomId, userId, searchTerm);

        validateUserCanAccessRoom(userId, roomId);
        validateSearchTerm(searchTerm);

        String cleanedTerm = searchTerm.trim();
        List<Message> searchResults = messageRepository.searchMessagesInRoom(roomId, cleanedTerm);

        log.debug("Found {} messages matching search term '{}' in room {}", searchResults.size(), cleanedTerm, roomId);
        return searchResults;
    }

    /**
     * Gets the last message sent in a room
     *
     * @param roomId ID of the chat room
     * @param userId ID of the requesting user
     * @return The last message, or null if no messages exist
     * @throws ResourceNotFoundException if user or room not found
     * @throws InsufficientPermissionException if user cannot access room
     */
    public Message getLastMessage(Long roomId, Long userId) {
        log.debug("Getting last message in room {} for user {}", roomId, userId);

        validateUserCanAccessRoom(userId, roomId);
        Message lastMessage = messageRepository.findLastMessageInRoom(roomId);

        if (lastMessage != null) {
            log.debug("Retrieved last message in room {}: {}", roomId, lastMessage.getId());
        } else {
            log.debug("No messages found in room {}", roomId);
        }

        return lastMessage;
    }

    /**
     * Deletes a message (sender or room admin only)
     *
     * @param messageId ID of the message to delete
     * @param userRequestingId ID of the user requesting deletion
     * @throws ResourceNotFoundException if message not found
     * @throws InsufficientPermissionException if user cannot delete message
     * @throws InvalidOperationException if deletion fails
     */
    public void deleteMessage(Long messageId, Long userRequestingId) {
        log.info("User {} attempting to delete message {}", userRequestingId, messageId);

        Message message = findMessageById(messageId);
        Long roomId = message.getChatRoomId();
        Long senderId = message.getSenderId();

        validateCanDeleteMessage(userRequestingId, senderId, roomId);

        try {
            messageRepository.deleteById(messageId);

            boolean isSender = senderId != null && senderId.equals(userRequestingId);
            String deletionInfo = isSender ? "deleted their message" : "message was deleted by admin";
            String systemMessageContent = "A " + deletionInfo;
            createSystemMessage(roomId, systemMessageContent);

            log.info("Message {} deleted successfully by user {}", messageId, userRequestingId);
        } catch (Exception e) {
            log.error("Failed to delete message {} by user {}: {}", messageId, userRequestingId, e.getMessage());
            throw InvalidOperationException.failedToDeleteMessage(messageId);
        }
    }

    /**
     * Gets all messages sent by a specific user in a room
     *
     * @param roomId ID of the chat room
     * @param targetUserId ID of the user whose messages to retrieve
     * @param requestingUserId ID of the user making the request
     * @return List of messages sent by the target user
     * @throws ResourceNotFoundException if users or room not found
     * @throws InsufficientPermissionException if requesting user cannot access room
     */
    public List<Message> getUserMessagesInRoom(Long roomId, Long targetUserId, Long requestingUserId) {
        log.debug("Getting messages by user {} in room {} for requesting user {}", targetUserId, roomId, requestingUserId);

        validateUserCanAccessRoom(requestingUserId, roomId);

        if (!userRepository.existsById(targetUserId)) {
            log.warn("Target user not found: {}", targetUserId);
            throw ResourceNotFoundException.userNotFound(targetUserId);
        }

        List<Message> userMessages = messageRepository.findUserMessagesInRoom(roomId, targetUserId);

        log.debug("Found {} messages by user {} in room {}", userMessages.size(), targetUserId, roomId);
        return userMessages;
    }

    // Private helper methods

    private void validateMessageContent(String content) {
        if (content == null || content.trim().isEmpty()) {
            throw ValidationException.messageContentRequired();
        }
        if (content.length() > 1000) {
            throw ValidationException.messageContentTooLong(1000);
        }
    }

    private void validateUserCanAccessRoom(Long senderId, Long roomId) {
        if (!userRepository.existsById(senderId)) {
            throw ResourceNotFoundException.userNotFound(senderId);
        }
        if (!chatRoomRepository.existsById(roomId)) {
            throw ResourceNotFoundException.chatRoomNotFound(roomId);
        }
        if (!chatParticipantRepository.isUserActiveInRoom(senderId, roomId)) {
            throw InsufficientPermissionException.cannotSendMessageToRoom(senderId, roomId);
        }
    }

    private void validateSearchTerm(String searchTerm) {
        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            throw ValidationException.searchTermRequired();
        }
        if (searchTerm.trim().length() < 2) {
            throw ValidationException.searchTermTooShort(2);
        }
    }

    private Message findMessageById(Long messageId) {
        return messageRepository.findById(messageId)
                .orElseThrow(() -> {
                    log.warn("Message not found: {}", messageId);
                    return ResourceNotFoundException.messageNotFound(messageId);
                });
    }

    private void validateCanDeleteMessage(Long userRequestingId, Long senderId, Long roomId) {
        boolean isSender = senderId != null && senderId.equals(userRequestingId);
        boolean isAdmin = chatParticipantRepository.isUserAdminInRoom(userRequestingId, roomId);

        if (!isSender && !isAdmin) {
            log.warn("User {} attempted to delete message they don't own and is not admin in room {}", userRequestingId, roomId);
            throw InsufficientPermissionException.cannotDeleteMessage(userRequestingId);
        }
    }

    private void updateRoomLastActivity(Long roomId) {
        Optional<ChatRoom> roomOpt = chatRoomRepository.findById(roomId);
        if (roomOpt.isPresent()) {
            ChatRoom room = roomOpt.get();
            room.setUpdatedAt(LocalDateTime.now());
            chatRoomRepository.save(room);
        }
    }

    private LocalDateTime getUserLastReadTime(Long userId, Long roomId) {
        Optional<ChatParticipant> participantOpt = chatParticipantRepository.findActiveParticipation(userId, roomId);

        if (participantOpt.isPresent()) {
            ChatParticipant participant = participantOpt.get();
            LocalDateTime lastReadAt = participant.getLastReadAt();
            return lastReadAt != null ? lastReadAt : LocalDateTime.of(1970, 1, 1, 0, 0);
        } else {
            log.warn("User {} is not an active participant in room {}", userId, roomId);
            throw InsufficientPermissionException.notActiveParticipant(userId, roomId);
        }
    }

    private void createSystemMessage(Long roomId, String content) {
        try {
            Message systemMessage = Message.builder()
                    .chatRoomId(roomId)
                    .content(content)
                    .messageType(MessageType.SYSTEM_MESSAGE)
                    .senderId(null)
                    .sentAt(LocalDateTime.now())
                    .build();

            messageRepository.save(systemMessage);
            log.debug("System message created in room {}: {}", roomId, content);
        } catch (Exception e) {
            log.error("Error creating system message in room {}: {}", roomId, e.getMessage());
            // Don't throw exception - system messages are not critical
        }
    }
}