package com.meng.lovespace.user.websocket;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.meng.lovespace.user.dto.MessageSendRequest;
import com.meng.lovespace.user.dto.ScheduledMessageCreateRequest;
import com.meng.lovespace.user.security.JwtUserPrincipal;
import com.meng.lovespace.user.service.CoupleBindingService;
import com.meng.lovespace.user.service.MessageService;
import com.meng.lovespace.user.util.JwtUtil;
import com.meng.lovespace.user.security.TokenBlacklistService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import java.io.IOException;
import java.net.URI;
import java.time.LocalDateTime;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * 私密消息聊天室 WebSocket：
 *
 * 客户端消息（JSON）：
 * - subscribe：{ "type":"subscribe", "coupleId":"..." }
 * - send：{ "type":"send", "coupleId":"...", "receiverId":"...", "content":"...", "messageType":"text|image|voice|letter" }
 * - read：{ "type":"read", "id":"..." }
 * - retract：{ "type":"retract", "id":"..." }
 *
 * 服务端推送：
 * - { "type":"privateMessage", "data": <PrivateMessageResponse> }
 */
@Component
public class ChatWebSocketHandler extends TextWebSocketHandler {

    private final JwtUtil jwtUtil;
    private final TokenBlacklistService blacklist;
    private final CoupleBindingService coupleBindingService;
    private final MessageService messageService;
    private final ChatSessionRegistry registry;
    private final ObjectMapper objectMapper;

    public ChatWebSocketHandler(
            JwtUtil jwtUtil,
            TokenBlacklistService blacklist,
            CoupleBindingService coupleBindingService,
            MessageService messageService,
            ChatSessionRegistry registry,
            ObjectMapper objectMapper) {
        this.jwtUtil = jwtUtil;
        this.blacklist = blacklist;
        this.coupleBindingService = coupleBindingService;
        this.messageService = messageService;
        this.registry = registry;
        this.objectMapper = objectMapper;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String token = tokenFromQuery(session.getUri());
        if (token == null || token.isBlank()) {
            session.close(CloseStatus.POLICY_VIOLATION);
            return;
        }

        try {
            Jws<Claims> jws = jwtUtil.parseAndValidate(token);
            Claims claims = jws.getPayload();
            String jti = jwtUtil.getJti(claims);
            if (blacklist.isBlacklisted(jti)) {
                session.close(CloseStatus.POLICY_VIOLATION);
                return;
            }
            JwtUserPrincipal principal =
                    new JwtUserPrincipal(
                            jwtUtil.getUserId(claims), jwtUtil.getUsername(claims), jwtUtil.getEmail(claims));
            registry.register(session, principal.userId());
        } catch (Exception e) {
            session.close(CloseStatus.POLICY_VIOLATION);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        registry.unregister(session);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String userId = registry.getUserId(session);
        if (userId == null || userId.isBlank()) {
            sendError(session, "unauthorized");
            return;
        }

        JsonNode root = objectMapper.readTree(message.getPayload());
        String type = root.path("type").asText(null);
        if (type == null) {
            sendError(session, "missing type");
            return;
        }

        try {
            switch (type) {
                case "subscribe" -> handleSubscribe(session, userId, root);
                case "send" -> handleSend(session, userId, root);
                case "scheduled" -> handleScheduledSend(session, userId, root);
                case "read" -> handleRead(session, userId, root);
                case "retract" -> handleRetract(session, userId, root);
                default -> sendError(session, "unknown type");
            }
        } catch (RuntimeException e) {
            sendError(session, e.getMessage());
        }
    }

    private void handleSubscribe(WebSocketSession session, String userId, JsonNode root) {
        String coupleId = root.path("coupleId").asText(null);
        if (coupleId == null || coupleId.isBlank()) {
            sendError(session, "coupleId is required");
            return;
        }
        boolean ok = coupleBindingService.findActiveOrFrozenMembership(userId, coupleId).isPresent();
        if (!ok) {
            sendError(session, "forbidden or invalid couple");
            return;
        }
        registry.subscribe(session, coupleId);
    }

    private void handleSend(WebSocketSession session, String userId, JsonNode root) {
        String coupleId = root.path("coupleId").asText(null);
        String receiverId = root.path("receiverId").asText(null);
        String content = root.path("content").asText(null);
        String messageType = root.path("messageType").asText(null);

        if (coupleId == null || receiverId == null || content == null || messageType == null) {
            throw new IllegalArgumentException("coupleId/receiverId/content/messageType are required");
        }

        MessageSendRequest req = new MessageSendRequest(coupleId, receiverId, content, messageType);
        messageService.send(userId, req);
    }

    private void handleRead(WebSocketSession session, String userId, JsonNode root) {
        String id = root.path("id").asText(null);
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("id is required");
        }
        messageService.markRead(userId, id);
    }

    private void handleScheduledSend(WebSocketSession session, String userId, JsonNode root) {
        String coupleId = root.path("coupleId").asText(null);
        String receiverId = root.path("receiverId").asText(null);
        String content = root.path("content").asText(null);
        String messageType = root.path("messageType").asText(null);
        String scheduledTimeRaw = root.path("scheduledTime").asText(null);

        if (coupleId == null
                || receiverId == null
                || content == null
                || messageType == null
                || scheduledTimeRaw == null) {
            throw new IllegalArgumentException(
                    "coupleId/receiverId/content/messageType/scheduledTime are required");
        }

        LocalDateTime scheduledTime;
        try {
            scheduledTime = LocalDateTime.parse(scheduledTimeRaw);
        } catch (Exception e) {
            throw new IllegalArgumentException("scheduledTime format invalid");
        }
        ScheduledMessageCreateRequest req =
                new ScheduledMessageCreateRequest(scheduledTime, coupleId, receiverId, content, messageType);
        messageService.createScheduled(userId, req);
    }

    private void handleRetract(WebSocketSession session, String userId, JsonNode root) {
        String id = root.path("id").asText(null);
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("id is required");
        }
        messageService.retract(userId, id);
    }

    private void sendError(WebSocketSession session, String msg) {
        if (session == null || !session.isOpen()) return;
        try {
            String payload = objectMapper.writeValueAsString(Map.of("type", "error", "message", msg));
            session.sendMessage(new TextMessage(payload));
        } catch (IOException ignored) {
            // ignore
        }
    }

    private static String tokenFromQuery(URI uri) {
        if (uri == null || uri.getQuery() == null) return null;
        try {
            return UriComponentsBuilder.fromUri(uri).build().getQueryParams().getFirst("token");
        } catch (Exception e) {
            return null;
        }
    }
}

