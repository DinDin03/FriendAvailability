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

@Service
@Transactional
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
    }

    public Message sendMessage(Long senderId, Long roomId, String content){
        validateUserCanAccessRoom(senderId, roomId);
        validateMessageContent(content);

        try{
            Message message = Message.builder()
            .senderId(senderId)
                .chatRoomId(roomId)
                .content(content.trim())
                .messageType(MessageType.TEXT)
                .sentAt(LocalDateTime.now())
                .build();
            Message savedMessage = messageRepository.save(message);
            updateRoomLastActivity(roomId);
            return savedMessage;
        }catch(Exception e){
            throw new RuntimeException("Failed to send message: " + e.getMessage(), e);

        }
    }

    private void validateMessageContent(String content) {
        if(content.trim().isEmpty() || content == null){
            throw new RuntimeException("Message can not be empty");
        }
        if(content.length() > 1000){
            throw new RuntimeException("Message can not exceed 1000 characters");
        }
    }

    private void validateUserCanAccessRoom(Long senderId, Long roomId) {
        if(!userRepository.existsById(senderId)){
            throw new RuntimeException("User not found with id " + senderId);
        }
        if(!chatRoomRepository.existsById(roomId)){
            throw new RuntimeException("Chat not found with id " + roomId);
        }
        if(!chatParticipantRepository.isUserActiveInRoom(senderId, roomId)){
            throw new RuntimeException("User " + senderId + " is not authorised to send messages to room " + roomId);
        }
    }

    private void updateRoomLastActivity(Long roomId){
        try{
            Optional<ChatRoom> roomOpt = chatRoomRepository.findById(roomId);
            if(roomOpt.isPresent()){
                ChatRoom room = roomOpt.get();
                room.setUpdatedAt(LocalDateTime.now());
                chatRoomRepository.save(room);
            }
        }catch(Exception e){
            System.out.println("Failed to update the room activity " + e.getMessage());
        }
    }

    public Page<Message> getMessageHistory(Long roomId, Long userId, int page, int size){
        
    }



    
}
