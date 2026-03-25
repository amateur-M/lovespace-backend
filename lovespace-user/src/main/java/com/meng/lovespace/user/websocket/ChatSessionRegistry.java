package com.meng.lovespace.user.websocket;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

/**
 * 管理 WebSocket 会话：
 *
 * - 连接时绑定 {@code userId}
 * - 客户端通过 subscribe 决定监听 {@code coupleId}
 * - 后端（HTTP/定时/WS）推送消息时按 coupleId 广播
 */
@Component
public class ChatSessionRegistry {

    private final ObjectMapper objectMapper;

    /** sessionId -> session */
    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    /** sessionId -> userId */
    private final Map<String, String> userBySessionId = new ConcurrentHashMap<>();
    /** sessionId -> coupleId（订阅的会话；未订阅则为 null） */
    private final Map<String, String> coupleBySessionId = new ConcurrentHashMap<>();

    public ChatSessionRegistry(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public void register(WebSocketSession session, String userId) {
        sessions.put(session.getId(), session);
        userBySessionId.put(session.getId(), userId);
        coupleBySessionId.remove(session.getId());
    }

    public void unregister(WebSocketSession session) {
        if (session == null) return;
        sessions.remove(session.getId());
        userBySessionId.remove(session.getId());
        coupleBySessionId.remove(session.getId());
    }

    public String getUserId(WebSocketSession session) {
        if (session == null) return null;
        return userBySessionId.get(session.getId());
    }

    public String getSubscribedCoupleId(WebSocketSession session) {
        if (session == null) return null;
        return coupleBySessionId.get(session.getId());
    }

    public void subscribe(WebSocketSession session, String coupleId) {
        if (session == null) return;
        coupleBySessionId.put(session.getId(), coupleId);
    }

    public void broadcastToCouple(String coupleId, Object dataPayload) {
        if (coupleId == null) return;
        String payloadJson;
        try {
            payloadJson =
                    objectMapper.writeValueAsString(
                            Map.of("type", "privateMessage", "data", dataPayload));
        } catch (JsonProcessingException e) {
            return;
        }

        for (WebSocketSession s : sessions.values()) {
            if (s == null || !Objects.equals(coupleId, coupleBySessionId.get(s.getId()))) continue;
            try {
                if (s.isOpen()) {
                    s.sendMessage(new TextMessage(payloadJson));
                }
            } catch (IOException e) {
                try {
                    s.close(CloseStatus.SERVER_ERROR);
                } catch (IOException ignored) {
                    // ignore
                }
                unregister(s);
            }
        }
    }
}

