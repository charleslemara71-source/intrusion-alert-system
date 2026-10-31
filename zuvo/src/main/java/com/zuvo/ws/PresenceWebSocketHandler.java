package com.zuvo.ws;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zuvo.service.PresenceService;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import java.util.Map;

@Component
public class PresenceWebSocketHandler extends TextWebSocketHandler {
    private final PresenceService presence;
    private final ObjectMapper mapper = new ObjectMapper();

    public PresenceWebSocketHandler(PresenceService presence) {
        this.presence = presence;
    }

    private Long uid(WebSocketSession s) {
        Object o = s.getAttributes().get("uid");
        return o instanceof Long l ? l : null;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        Long uid = uid(session);
        if (uid == null) {
            session.close(CloseStatus.POLICY_VIOLATION);
            return;
        }
        presence.add(uid, session);
        presence.sendRaw(session, Map.of("type", "hello", "online", presence.onlineIds()));
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        JsonNode n = mapper.readTree(message.getPayload());
        if ("ping".equals(n.path("type").asText())) {
            presence.ping(session);
            presence.sendRaw(session, Map.of("type", "pong"));
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        Long uid = uid(session);
        if (uid != null) presence.remove(uid, session);
    }
}
