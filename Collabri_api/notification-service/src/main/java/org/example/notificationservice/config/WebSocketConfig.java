package org.example.notificationservice.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * WebSocket configuration for real-time notifications using STOMP over WebSocket.
 *
 * Configuration details:
 * - STOMP endpoint: /ws (with SockJS fallback for older browsers)
 * - Message broker: Simple in-memory broker for /user and /topic destinations
 * - Application destination prefix: /app
 * - User destination prefix: /user (Spring default)
 *
 * Security:
 * - JWT validation is performed in JwtChannelInterceptor on CONNECT
 * - Each user can only subscribe to their own queue: /user/{userId}/queue/notifications
 */
@Slf4j
@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final JwtChannelInterceptor jwtChannelInterceptor;

    /**
     * Register STOMP endpoints for WebSocket connections.
     * Clients connect to ws://host:port/ws
     */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*");

        log.info("WebSocket STOMP endpoint registered at /ws (native WebSocket)");
    }

    /**
     * Configure message broker for routing messages.
     * - /user prefix: for user-specific messages (point-to-point)
     * - /topic prefix: for broadcast messages (optional, not used initially)
     * - /app prefix: for messages sent from clients to server
     */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // Enable simple in-memory broker for user-specific and topic destinations
        registry.enableSimpleBroker("/user", "/topic");

        // Prefix for messages from clients to server-side @MessageMapping methods
        registry.setApplicationDestinationPrefixes("/app");

        // User destination prefix (default is /user, but explicit for clarity)
        registry.setUserDestinationPrefix("/user");

        log.info("Message broker configured with /user and /topic prefixes");
    }

    /**
     * Register channel interceptor for JWT authentication on CONNECT.
     * This ensures only authenticated users can establish WebSocket connections.
     */
    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(jwtChannelInterceptor);
        log.info("JWT channel interceptor registered for WebSocket authentication");
    }
}
