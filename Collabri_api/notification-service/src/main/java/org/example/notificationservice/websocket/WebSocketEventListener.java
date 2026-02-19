package org.example.notificationservice.websocket;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;
import org.springframework.web.socket.messaging.SessionUnsubscribeEvent;

/**
 * Event listener for WebSocket lifecycle events.
 * Handles connection, disconnection, subscription, and unsubscription events.
 *
 * Provides logging and monitoring capabilities for WebSocket sessions.
 * Can be extended to track active connections, implement presence features, etc.
 */
@Slf4j
@Component
public class WebSocketEventListener {

    /**
     * Helper method to extract userId from session attributes or authentication principal
     */
    private String extractUserId(StompHeaderAccessor headerAccessor) {
        // First, try to get from session attributes
        String userId = (String) headerAccessor.getSessionAttributes().get("userId");

        // Fallback: get from authenticated principal (set in JwtChannelInterceptor)
        if (userId == null && headerAccessor.getUser() != null) {
            userId = headerAccessor.getUser().getName();
        }

        return userId;
    }

    /**
     * Handle WebSocket connection established event.
     * Triggered after successful CONNECT and authentication.
     */
    @EventListener
    public void handleWebSocketConnectListener(SessionConnectedEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();
        String userId = extractUserId(headerAccessor);

        log.info("WebSocket connection established - sessionId: {}, userId: {}", sessionId, userId);

        // Optional: Track active connections in a concurrent map for presence/monitoring
        // Example: activeConnections.put(userId, sessionId);
    }

    /**
     * Handle WebSocket disconnection event.
     * Triggered when client disconnects or connection is lost.
     */
    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();
        String userId = extractUserId(headerAccessor);

        log.info("WebSocket connection closed - sessionId: {}, userId: {}", sessionId, userId);

        // Optional: Clean up active connection tracking
        // Example: activeConnections.remove(userId);
    }

    /**
     * Handle subscription event.
     * Triggered when client subscribes to a destination.
     */
    @EventListener
    public void handleWebSocketSubscribeListener(SessionSubscribeEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();
        String userId = extractUserId(headerAccessor);
        String destination = headerAccessor.getDestination();

        log.debug("WebSocket subscription - sessionId: {}, userId: {}, destination: {}",
                 sessionId, userId, destination);

        // Optional: Validate subscription destination matches userId
        // Prevent users from subscribing to other users' queues
        if (destination != null && destination.contains("/queue/notifications")) {
            if (userId != null && !destination.contains(userId)) {
                log.warn("Attempted unauthorized subscription - userId: {}, destination: {}",
                        userId, destination);
                // Note: Spring Security will handle this, but logging for monitoring
            }
        }
    }

    /**
     * Handle unsubscription event.
     * Triggered when client unsubscribes from a destination.
     */
    @EventListener
    public void handleWebSocketUnsubscribeListener(SessionUnsubscribeEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();
        String userId = extractUserId(headerAccessor);

        log.debug("WebSocket unsubscription - sessionId: {}, userId: {}", sessionId, userId);
    }
}

