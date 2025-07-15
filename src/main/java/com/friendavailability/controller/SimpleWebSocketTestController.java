package com.friendavailability.controller;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.time.LocalDateTime;
import java.util.Map;

@Controller
public class SimpleWebSocketTestController {

    private final SimpMessagingTemplate messagingTemplate;

    public SimpleWebSocketTestController(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    @MessageMapping("/test.message")
    public void handleTestMessage(@Payload Map<String, Object> message) {
        try {
            // Extract basic info from the message
            String content = (String) message.get("content");
            String senderName = (String) message.get("senderName");
            String roomId = String.valueOf(message.get("roomId"));
            
            // Create a simple response
            Map<String, Object> response = Map.of(
                "id", System.currentTimeMillis(),
                "senderName", senderName != null ? senderName : "Unknown User",
                "roomId", roomId,
                "content", content != null ? content : "Empty message",
                "messageType", "TEXT",
                "sentAt", LocalDateTime.now().toString()
            );
            
            // Broadcast to everyone subscribed to this room
            String destination = "/topic/test/room/" + roomId;
            messagingTemplate.convertAndSend(destination, response);
            
            System.out.println("✅ Test message sent to: " + destination);
            System.out.println("📝 Content: " + content);
            
        } catch (Exception e) {
            System.err.println("❌ Error handling test message: " + e.getMessage());
            
            // Send error back to sender
            Map<String, Object> errorResponse = Map.of(
                "error", "Failed to send message: " + e.getMessage(),
                "timestamp", LocalDateTime.now().toString(),
                "errorCode", "TEST_MESSAGE_FAILED"
            );
            
            messagingTemplate.convertAndSend("/topic/test/errors", errorResponse);
        }
    }

    @MessageMapping("/test.join")
    public void handleTestJoin(@Payload Map<String, Object> joinData) {
        try {
            String userName = (String) joinData.get("userName");
            String roomId = String.valueOf(joinData.get("roomId"));
            
            // Create join notification
            Map<String, Object> joinNotification = Map.of(
                "roomId", roomId,
                "content", (userName != null ? userName : "Someone") + " joined the chat",
                "messageType", "SYSTEM_MESSAGE",
                "timestamp", LocalDateTime.now().toString()
            );
            
            String destination = "/topic/test/room/" + roomId;
            messagingTemplate.convertAndSend(destination, joinNotification);
            
            System.out.println("✅ User joined: " + userName + " in room: " + roomId);
            
        } catch (Exception e) {
            System.err.println("❌ Error handling join: " + e.getMessage());
        }
    }

    @MessageMapping("/test.leave")
    public void handleTestLeave(@Payload Map<String, Object> leaveData) {
        try {
            String userName = (String) leaveData.get("userName");
            String roomId = String.valueOf(leaveData.get("roomId"));
            
            // Create leave notification
            Map<String, Object> leaveNotification = Map.of(
                "roomId", roomId,
                "content", (userName != null ? userName : "Someone") + " left the chat",
                "messageType", "SYSTEM_MESSAGE", 
                "timestamp", LocalDateTime.now().toString()
            );
            
            String destination = "/topic/test/room/" + roomId;
            messagingTemplate.convertAndSend(destination, leaveNotification);
            
            System.out.println("✅ User left: " + userName + " from room: " + roomId);
            
        } catch (Exception e) {
            System.err.println("❌ Error handling leave: " + e.getMessage());
        }
    }

    @MessageMapping("/test.typing")
    public void handleTestTyping(@Payload Map<String, Object> typingData) {
        try {
            String userName = (String) typingData.get("userName");
            String roomId = String.valueOf(typingData.get("roomId"));
            Boolean isTyping = (Boolean) typingData.get("isTyping");
            
            Map<String, Object> typingResponse = Map.of(
                "userName", userName != null ? userName : "Someone",
                "roomId", roomId,
                "isTyping", isTyping != null ? isTyping : false,
                "timestamp", LocalDateTime.now().toString()
            );
            
            String destination = "/topic/test/room/" + roomId + "/typing";
            messagingTemplate.convertAndSend(destination, typingResponse);
            
            System.out.println("✅ Typing indicator: " + userName + " is " + 
                             (isTyping ? "typing" : "not typing"));
            
        } catch (Exception e) {
            System.err.println("❌ Error handling typing: " + e.getMessage());
        }
    }
}