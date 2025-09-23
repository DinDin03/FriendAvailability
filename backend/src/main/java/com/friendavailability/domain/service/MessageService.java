package com.friendavailability.domain.service;

import com.friendavailability.domain.entity.*;
import com.friendavailability.domain.entity.enums.MessageType;
import com.friendavailability.domain.exception.InsufficientPermissionException;
import com.friendavailability.domain.exception.ResourceNotFoundException;
import com.friendavailability.domain.exception.ValidationException;
import com.friendavailability.domain.repository.*;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
@Slf4j
public class MessageService {

    private final MessageRepository messageRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final ChatParticipantRepository chatParticipantRepository;
    private final UserRepository userRepository;

    public MessageService(MessageRepository messageRepository, ChatRoomRepository chatRoomRepository
    ,ChatParticipantRepository chatParticipantRepository, UserRepository userRepository){
        this.messageRepository = messageRepository;
        this.chatParticipantRepository = chatParticipantRepository;
        this.chatRoomRepository = chatRoomRepository;
        this.userRepository = userRepository;
        log.info("MessageService initialised successfully");
    }

    public Message sendMessage(Long senderId, Long roomId, String content){
        validateUserCanAccessRoom(senderId, roomId);
        validateMessageContent(content);

        Message message = Message.builder()
        .senderId(senderId)
            .chatRoomId(roomId)
            .content(content.trim())
            .messageType(MessageType.TEXT)
            .sentAt(LocalDateTime.now())
            .build();
        Message savedMessage = messageRepository.save(message);

        log.info("Message sent successfully from user {} to room {}", senderId, roomId);
        updateRoomLastActivity(roomId);
        return savedMessage;
    }

    private void validateMessageContent(String content) {
        if(content == null || content.trim().isEmpty()){
            throw ValidationException.messageContentRequired();
        }
        if(content.length() > 1000){
            throw ValidationException.messageContentTooLong(1000);
        }
    }

    private void validateUserCanAccessRoom(Long senderId, Long roomId) {
        if(!userRepository.existsById(senderId)){
            throw ResourceNotFoundException.userNotFound(senderId);
        }
        if(!chatRoomRepository.existsById(roomId)){
            throw ResourceNotFoundException.chatRoomNotFound(roomId);
        }
        if(!chatParticipantRepository.isUserActiveInRoom(senderId, roomId)){
            throw InsufficientPermissionException.cannotSendMessageToRoom(senderId, roomId);
        }
    }

    private void updateRoomLastActivity(Long roomId){

        Optional<ChatRoom> roomOpt = chatRoomRepository.findById(roomId);
        if(roomOpt.isPresent()){
            ChatRoom room = roomOpt.get();
            room.setUpdatedAt(LocalDateTime.now());
            chatRoomRepository.save(room);
        }
    }

    public Page<Message> getMessageHistory(Long roomId, Long userId, int page, int size){
        validateUserCanAccessRoom(userId, roomId);
        Pageable pageable = PageRequest.of(page, size);
        Page<Message> messagePage = messageRepository.findByChatRoomIdOrderBySentAtDesc(roomId, pageable);

        return messagePage;
    }

    public List<Message> getRecentMessages(Long roomId, Long userId, int limit){
        validateUserCanAccessRoom(userId, roomId);
        List<Message> recentMessages = messageRepository.findRecentMessagesInRoom(roomId, limit);
        return recentMessages;
    }

    public List<Message> getMessagesAfterTime(Long userId, Long roomId, LocalDateTime afterTime){
        validateUserCanAccessRoom(userId, roomId); 
        List<Message> newMessages = messageRepository.findMessagesAfterTime(roomId, afterTime);
        return newMessages;
    }
    
    public long getUnreadMessagesCount(Long roomId, Long userId){
        validateUserCanAccessRoom(userId, roomId);
        LocalDateTime lastReadTime = getUserLastReadTime(userId, roomId);
        
        long unreadCount = messageRepository.countUnreadMessages(roomId, userId, lastReadTime);
        return unreadCount;
    }

    public List<Message> getUnreadMessages(Long roomId, Long userId){
        validateUserCanAccessRoom(userId, roomId);
        LocalDateTime lastReadTime = getUserLastReadTime(userId, roomId);
        
        List<Message> unreadMessages = messageRepository.findUnreadMessages(roomId, userId, lastReadTime);
        return unreadMessages;
    }

    public void markMessagesAsRead(Long roomId, Long userId){
        validateUserCanAccessRoom(userId, roomId);

        try{
            LocalDateTime now = LocalDateTime.now();
            chatParticipantRepository.updateLastReadTime(userId, roomId, now);
        }catch(Exception e){
            throw new RuntimeException("Failed to mark messages as read: " + e.getMessage(), e);
        }
    }

    public long getTotalUnreadCount(Long userId){
        if(!userRepository.existsById(userId)){
            throw new RuntimeException("User not found with ID: " + userId);
        }
        long unreadCount = messageRepository.countTotalUnreadMessagesForUser(userId);
        return unreadCount;
    }

    private LocalDateTime getUserLastReadTime(Long userId, Long roomId){
        try{
            Optional<ChatParticipant> pOptional = chatParticipantRepository.findActiveParticipation(userId, roomId);

            if(pOptional.isPresent()){
                ChatParticipant participant = pOptional.get();
                LocalDateTime lastReadAt = participant.getLastReadAt();

                if (lastReadAt == null) {
                    lastReadAt = LocalDateTime.of(1970, 1, 1, 0, 0); // Unix epoch
                }
                return lastReadAt;
            }else{
                throw new RuntimeException("User " + userId + " is not a participant in room " + roomId);
            }
        }catch(Exception e){
            return LocalDateTime.of(1970, 1, 1, 0, 0);
        }
    }

    public List<Message> searchMessages(Long roomId, Long userId, String searchTerm){
        validateUserCanAccessRoom(userId, roomId);

        if(searchTerm == null || searchTerm.trim().isEmpty()){
            return List.of();
        }
        if(searchTerm.trim().length() < 2){
            throw new RuntimeException("Search term must be at least 2 characters");
        }

        String cleanedTerm = searchTerm.trim();
        List<Message> searchResults = messageRepository.searchMessagesInRoom(roomId, cleanedTerm);

        return searchResults;
    }

    public Message getLastMessage(Long roomId, Long userId){
        validateUserCanAccessRoom(userId, roomId);

        Message lastMessage = messageRepository.findLastMessageInRoom(roomId);

        return lastMessage;
    }

    public void deleteMessage(Long messageId, Long userRequestingId){
        Message message = messageRepository.findById(messageId).orElseThrow(() -> new RuntimeException("Message not found with ID: " + messageId));
        
        Long roomId = message.getChatRoomId();
        Long senderId = message.getSenderId();

        boolean isSender = senderId.equals(userRequestingId) && senderId != null;
        boolean isAdmin = chatParticipantRepository.isUserAdminInRoom(userRequestingId, roomId);

        if (!isSender && !isAdmin) {
            throw new RuntimeException("Only message sender or room admin can delete messages");
        }

        try{
            messageRepository.deleteById(messageId);
            String deletionInfo = isSender ? "deleted their message" : "message was deleted by admin";
            String systemMessageContent = "A " + deletionInfo;
            createSystemMessage(roomId, systemMessageContent);
        }catch(Exception e){
            throw new RuntimeException("Failed to delete message: " + e.getMessage(), e);
        }
    }
    public List<Message> getUserMessagesInRoom(Long roomId, Long targetUserId, Long requestingUserId) {        
        validateUserCanAccessRoom(requestingUserId, roomId);
        
        if (!userRepository.existsById(targetUserId)) {
            throw new RuntimeException("Target user not found with ID: " + targetUserId);
        }
        
        List<Message> userMessages = messageRepository.findUserMessagesInRoom(roomId, targetUserId);
        
        System.out.println("Found " + userMessages.size() + " messages by user " + targetUserId + " in room " + roomId);
        return userMessages;
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
            
        } catch (Exception e) {
            System.err.println("Error creating system message: " + e.getMessage());
        }
    }

}
