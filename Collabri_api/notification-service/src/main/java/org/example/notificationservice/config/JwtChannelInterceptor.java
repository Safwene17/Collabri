package org.example.notificationservice.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Interceptor to validate JWT tokens on WebSocket CONNECT.
 *
 * Security flow:
 * 1. Client sends CONNECT frame with JWT in "Authorization" header (Bearer token)
 * 2. Interceptor extracts and validates the JWT using JwtDecoder
 * 3. Extracts userId from JWT claims and stores in session attributes
 * 4. Sets authenticated user principal for the WebSocket session
 * 5. Subsequent messages in the session are associated with this userId
 *
 * Session attributes:
 * - "userId": UUID string extracted from JWT, used for routing notifications
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtChannelInterceptor implements ChannelInterceptor {

    private final JwtDecoder jwtDecoder;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        // Create a mutable accessor to properly set session attributes
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);

        // Log all STOMP commands for debugging
        if (accessor != null) {
            log.debug("STOMP {} command received from session: {}", accessor.getCommand(), accessor.getSessionId());

            if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
                String destination = accessor.getDestination();
                String sessionId = accessor.getSessionId();
                String userId = (String) accessor.getSessionAttributes().get("userId");
                log.info("STOMP SUBSCRIBE - sessionId: {}, userId: {}, destination: {}", sessionId, userId, destination);
            }

            if (StompCommand.DISCONNECT.equals(accessor.getCommand())) {
                String sessionId = accessor.getSessionId();
                String userId = (String) accessor.getSessionAttributes().get("userId");
                log.info("STOMP DISCONNECT - sessionId: {}, userId: {}", sessionId, userId);
            }
        }

        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            // Extract JWT from Authorization header
            String authHeader = accessor.getFirstNativeHeader("Authorization");

            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                log.warn("WebSocket CONNECT attempt without valid Authorization header");
                throw new IllegalArgumentException("Missing or invalid Authorization header. Expected: Bearer <token>");
            }

            String token = authHeader.substring(7); // Remove "Bearer " prefix

            try {
                // Decode and validate JWT
                Jwt jwt = jwtDecoder.decode(token);

                // Extract userId from JWT claims
                String userId = jwt.getClaimAsString("userId");

                if (userId == null || userId.isBlank()) {
                    log.warn("JWT does not contain userId claim");
                    throw new IllegalArgumentException("JWT missing userId claim");
                }

                // Extract roles for authorities
                List<String> roles = jwt.getClaimAsStringList("roles");
                List<SimpleGrantedAuthority> authorities = roles != null
                    ? roles.stream()
                        .map(role -> new SimpleGrantedAuthority(role.startsWith("ROLE_") ? role : "ROLE_" + role))
                        .collect(Collectors.toList())
                    : List.of();

                // Create authentication token with userId as principal
                UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(userId, null, authorities);

                // Set authenticated user in WebSocket session
                accessor.setUser(authentication);

                // Store userId in session attributes for easy access in event listeners
                accessor.getSessionAttributes().put("userId", userId);

                log.info("WebSocket authentication successful for userId: {} in session: {}", userId, accessor.getSessionId());

            } catch (JwtException e) {
                log.error("JWT validation failed for WebSocket connection: {}", e.getMessage());
                throw new IllegalArgumentException("Invalid JWT token: " + e.getMessage());
            } catch (Exception e) {
                log.error("Unexpected error during WebSocket authentication: {}", e.getMessage(), e);
                throw new IllegalArgumentException("Authentication failed: " + e.getMessage());
            }
        }

        return message;
    }

    /**
     * Called after message is sent. Can be used for logging or cleanup.
     */
    @Override
    public void postSend(Message<?> message, MessageChannel channel, boolean sent) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand()) && sent) {
            String userId = (String) accessor.getSessionAttributes().get("userId");
            String sessionId = accessor.getSessionId();
            log.debug("WebSocket session established - sessionId: {}, userId: {}", sessionId, userId);
        }
    }
}

