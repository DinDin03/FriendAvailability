package com.friendavailability.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        System.out.println("🔌 Configuring WebSocket STOMP endpoints");
        
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .withSockJS();
        
        System.out.println("✅ WebSocket endpoint registered at: /ws");
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        System.out.println("📮 Configuring message broker");
        
        registry.enableSimpleBroker("/topic", "/queue");
        registry.setApplicationDestinationPrefixes("/app");
        registry.setUserDestinationPrefix("/user");
        
        System.out.println("✅ Message broker configured:");
        System.out.println("   📢 /topic/* - For broadcast messages");
        System.out.println("   📨 /queue/* - For targeted messages");
        System.out.println("   👤 /user/* - For user-specific destinations");
        System.out.println("   🎯 /app/* - For incoming messages");
    }
}