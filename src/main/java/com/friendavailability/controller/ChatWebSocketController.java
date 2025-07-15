package com.friendavailability.controller;

import com.friendavailability.dto.chat.*;
import com.friendavailability.model.Message;
import com.friendavailability.service.ChatService;
import com.friendavailability.service.MessageService;
import com.friendavailability.service.UserService;

import org.springframework.context.event.EventListener;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.time.LocalDateTime;

@Controller
public class ChatWebSocketController {

    private final MessageService messageService;
    private final ChatService chatService;
    private final SimpMessagingTemplate messagingTemplate;
    private final UserService userService;

    public ChatWebSocketController(MessageService messageService, 
                                 ChatService chatService,
                                 SimpMessagingTemplate messagingTemplate, 
                                 UserService userService) {
        this.messageService = messageService;
        this.chatService = chatService;
        this.messagingTemplate = messagingTemplate;
        this.userService = userService;
    }

    @MessageMapping("/chat.sendMessage")
    public void sendMessage(@Payload ChatMessageDto chatMessage) {
        try {
            Message savedMessage = messageService.sendMessage(
                chatMessage.getSenderId(),
                chatMessage.getRoomId(),
                chatMessage.getContent()
            );

            MessageResponseDto response = MessageResponseDto.builder()
                .id(savedMessage.getId())
                .senderId(savedMessage.getSenderId())
                .senderName(savedMessage.getSenderName())
                .roomId(savedMessage.getChatRoomId())
                .content(savedMessage.getContent())
                .messageType(savedMessage.getMessageType().toString())
                .sentAt(savedMessage.getSentAt())
                .build();
            
            String destination = "/topic/chat/" + chatMessage.getRoomId();
            messagingTemplate.convertAndSend(destination, response);
            
        } catch (Exception e) {
            sendErrorToUser(chatMessage.getSenderId(), "MESSAGE_SEND_FAILED", 
                          "Failed to send message: " + e.getMessage());
        }
    }

    @MessageMapping("/chat.connectToChat")
    public void connectToChat(@Payload UserJoinDto userJoin) {
        try {
            chatService.getChatRoom(userJoin.getRoomId(), userJoin.getUserId());

            String userName = getSenderName(userJoin.getUserId());
            String systemContent = userName + " joined the chat";

            SystemMessageDto systemMessage = SystemMessageDto.builder()
                .roomId(userJoin.getRoomId())
                .content(systemContent)
                .messageType("SYSTEM_MESSAGE")
                .timestamp(LocalDateTime.now())
                .build();
            
            String destination = "/topic/chat/" + userJoin.getRoomId();
            messagingTemplate.convertAndSend(destination, systemMessage);
            
        } catch (Exception e) {
            sendErrorToUser(userJoin.getUserId(), "USER_JOIN_FAILED", 
                          "Failed to join chat: " + e.getMessage());
        }
    }

    @MessageMapping("/chat.disconnectFromChat")
    public void disconnectFromChat(@Payload UserLeaveDto userLeave) {        
        try {
            String userName = getSenderName(userLeave.getUserId());
            String systemContent = userName + " left the chat";
            
            SystemMessageDto systemMessage = SystemMessageDto.builder()
                .roomId(userLeave.getRoomId())
                .content(systemContent)
                .messageType("SYSTEM_MESSAGE")
                .timestamp(LocalDateTime.now())
                .build();
            
            String destination = "/topic/chat/" + userLeave.getRoomId();
            messagingTemplate.convertAndSend(destination, systemMessage);
            
        } catch (Exception e) {
            System.err.println("Error handling user temporary leave: " + e.getMessage());
        }
    }

    @MessageMapping("/chat.leaveGroup")
    public void leaveGroup(@Payload UserLeaveDto userLeave) {
        try {
            chatService.removeUsersFromChat( 
                userLeave.getRoomId(), 
                userLeave.getUserId(), 
                userLeave.getUserId() 
            );
            
            String userName = getSenderName(userLeave.getUserId());
            String systemContent = userName + " left the group";
            
            SystemMessageDto systemMessage = SystemMessageDto.builder()
                .roomId(userLeave.getRoomId())
                .content(systemContent)
                .messageType("SYSTEM_MESSAGE")
                .timestamp(LocalDateTime.now())
                .build();
            
            String destination = "/topic/chat/" + userLeave.getRoomId();
            messagingTemplate.convertAndSend(destination, systemMessage);
            
        } catch (Exception e) {
            sendErrorToUser(userLeave.getUserId(), "GROUP_LEAVE_FAILED", 
                          "Failed to leave group: " + e.getMessage());
        }
    }

    @MessageMapping("/chat.typing")
    public void handleTyping(@Payload TypingIndicatorDto typingData) {
        try {
            chatService.getChatRoom(typingData.getRoomId(), typingData.getUserId());

            String userName = getSenderName(typingData.getUserId());

            TypingResponseDto typingResponse = TypingResponseDto.builder()
                .userId(typingData.getUserId())
                .userName(userName)
                .roomId(typingData.getRoomId())
                .isTyping(typingData.isTyping()) 
                .timestamp(LocalDateTime.now())
                .build();
            
            String destination = "/topic/chat/" + typingData.getRoomId() + "/typing";
            messagingTemplate.convertAndSend(destination, typingResponse);
            
        } catch (Exception e) {
            System.err.println("Error handling typing indicator: " + e.getMessage());
        }
    }

    @MessageMapping("/chat.markAsRead")
    public void markAsRead(@Payload MessageReadDto readData) {
        try {
            chatService.getChatRoom(readData.getRoomId(), readData.getUserId());
            messageService.markMessagesAsRead(readData.getRoomId(), readData.getUserId());

            ReadReceiptDto readReceipt = ReadReceiptDto.builder()
                .userId(readData.getUserId())
                .roomId(readData.getRoomId())
                .readAt(LocalDateTime.now())
                .build();

            String destination = "/topic/chat/" + readData.getRoomId() + "/read";
            messagingTemplate.convertAndSend(destination, readReceipt);
            
        } catch (Exception e) {
            sendErrorToUser(readData.getUserId(), "MARK_READ_FAILED", 
                          "Failed to mark messages as read: " + e.getMessage());
        }
    }

    @EventListener
    public void handleWebSocketDisconnect(SessionDisconnectEvent event) {
        try {
            System.out.println("WebSocket session disconnected: " + event.getSessionId());
        } catch (Exception e) {
            System.err.println("Error handling WebSocket disconnect: " + e.getMessage());
        }
    }

    private String getSenderName(Long userId) {
        return userService.findUserById(userId).get().getName();
    }

    private void sendErrorToUser(Long userId, String errorCode, String errorMessage) {
        ErrorMessageDto errorResponse = ErrorMessageDto.builder()
            .error(errorMessage)
            .timestamp(LocalDateTime.now())
            .errorCode(errorCode)
            .build();
        
        messagingTemplate.convertAndSendToUser(
            userId.toString(),  
            "/queue/errors",
            errorResponse
        );
    }
}