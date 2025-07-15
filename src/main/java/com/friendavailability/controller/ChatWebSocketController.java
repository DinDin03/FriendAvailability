import com.friendavailability.dto.chat.*;
import com.friendavailability.model.Message;
import com.friendavailability.service.ChatService;
import com.friendavailability.service.MessageService;
import com.friendavailability.service.UserService;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.time.LocalDateTime;

@Controller
public class ChatWebSocketController {

    private final MessageService messageService;
    private final ChatService chatService;
    private final SimpMessagingTemplate messagingTemplate;
    private final UserService userService;

    public ChatWebSocketController(MessageService messageService, ChatService chatService,
            SimpMessagingTemplate messagingTemplate, UserService userService) {
        this.messageService = messageService;
        this.chatService = chatService;
        this.messagingTemplate = messagingTemplate;
        this.userService = userService;
    }

    @MessageMapping("/chat.sendMessage")
    public void handleMessage(@Payload ChatMessageDto chatMessage) {
        try{
            Message savedMessage = messageService.sendMessage(
                chatMessage.getSenderId(),
                chatMessage.getRoomId(),
                chatMessage.getContent());

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
        }catch(Exception e){
            ErrorMessageDto errorResponse = ErrorMessageDto.builder()
                .error("Failed to send message: " + e.getMessage())
                .timestamp(LocalDateTime.now())
                .errorCode("MESSAGE_SEND_FAILED")
                .build();
            
            messagingTemplate.convertAndSendToUser(
                chatMessage.getSenderId().toString(),  
                "/queue/errors",
                errorResponse
            );
        }
    }

    @MessageMapping("/chat.addUser")
    public void addUser(@Payload UserJoinDto userJoin){
        try{
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
            messagingTemplate.convertAndSend(destination, systemContent);
        }catch(Exception e){
            ErrorMessageDto errorResponse = ErrorMessageDto.builder()
                .error("Failed to join chat: " + e.getMessage())
                .timestamp(LocalDateTime.now())
                .errorCode("USER_JOIN_FAILED")
                .build();
            
            messagingTemplate.convertAndSendToUser(
                userJoin.getUserId().toString(),
                "/queue/errors",
                errorResponse);
        }
    }

    private String getSenderName(Long userId){
        return userService.findUserById(userId).get().getName();
    }

    @MessageMapping("/chat.removeUser")
    public void removeUser(@Payload UserLeaveDto userLeave) {        
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
            
            System.out.println("User temporary leave notification sent to " + destination);
            System.out.println("   Note: User remains in database and can rejoin automatically");
            
        } catch (Exception e) {
            System.err.println("Error handling user leave: " + e.getMessage());
        }
    }

    @MessageMapping("/chat.leaveGroup")
    public void leaveGroup(@Payload UserLeaveDto userLeave) {
        System.out.println("🚪 User " + userLeave.getUserId() + " permanently leaving group " + userLeave.getRoomId());
        
        try {
            chatService.removeUsersFromChat( 
                userLeave.getRoomId(), 
                userLeave.getUserId(), 
                userLeave.getUserId() 
            );
            
            System.out.println("✅ User " + userLeave.getUserId() + " removed from database");
            
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
            
            System.out.println("User permanent leave notification sent to " + destination);
            System.out.println("   User must be re-invited to rejoin this group");
            
        } catch (Exception e) {
            System.err.println("Error handling permanent group leave: " + e.getMessage());
            
            ErrorMessageDto errorResponse = ErrorMessageDto.builder()
                .error("Failed to leave group: " + e.getMessage())
                .timestamp(LocalDateTime.now())
                .errorCode("GROUP_LEAVE_FAILED")
                .build();
            
            messagingTemplate.convertAndSendToUser(
                userLeave.getUserId().toString(),
                "/queue/errors",
                errorResponse
            );
        }
    }

    @MessageMapping("/chat.typing")
    public void handleTyping(@Payload TypingIndicatorDto typingData){
        try{
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
        }catch(Exception e){
            System.err.println("Error handling typing indicator: " + e.getMessage());
        }
    }
}
