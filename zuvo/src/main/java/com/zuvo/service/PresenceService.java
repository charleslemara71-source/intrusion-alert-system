package com.zuvo.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks who is connected (the GREEN online indicator) and pushes real-time events
 * (new message, follow request, live request ...) to connected users.
 */
@Service
public class PresenceService {
    private static final Logger log = LoggerFactory.getLogger(PresenceService.class);
    private static final long STALE_MS = 75_000; // no heartbeat for 75s = offline

    private final Map<Long, Set<WebSocketSession>> sessions = new ConcurrentHashMap<>();
    private final Map<String, Long> lastPing = new ConcurrentHashMap<>();
    private final ObjectMapper mapper = new ObjectMapper();

    public boolean isOnline(Long userId) {
        Set<WebSocketSession> s = sessions.get(userId);
        return s != null && !s.isEmpty();
    }

    public Set<Long> onlineIds() {
        return new HashSet<>(sessions.keySet());
    }

    public void add(Long userId, WebSocketSession session) {
        Set<WebSocketSession> set = sessions.computeIfAbsent(userId, k -> ConcurrentHashMap.newKeySet());
        boolean wasOffline = set.isEmpty();
        set.add(session);
        lastPing.put(session.getId(), System.currentTimeMillis());
        if (wasOffline) broadcast(Map.of("type", "presence", "userId", userId, "online", true));
    }

    public void remove(Long userId, WebSocketSession session) {
        lastPing.remove(session.getId());
        Set<WebSocketSession> set = sessions.get(userId);
        if (set == null) return;
        set.remove(session);
        if (set.isEmpty()) {
            sessions.remove(userId);
            broadcast(Map.of("type", "presence", "userId", userId, "online", false));
        }
    }

    public void ping(WebSocketSession session) {
        lastPing.put(session.getId(), System.currentTimeMillis());
    }

    /** Send an event to one user (all their open tabs/devices). */
    public void sendTo(Long userId, Object payload) {
        Set<WebSocketSession> set = sessions.get(userId);
        if (set == null) return;
        String json = toJson(payload);
        for (WebSocketSession s : set) send(s, json);
    }

    /** Send an event to everyone who is connected. */
    public void broadcast(Object payload) {
        String json = toJson(payload);
        for (Set<WebSocketSession> set : sessions.values())
            for (WebSocketSession s : set) send(s, json);
    }

    public void sendRaw(WebSocketSession s, Object payload) {
        send(s, toJson(payload));
    }

    private String toJson(Object o) {
        try { return mapper.writeValueAsString(o); } catch (Exception e) { return "{}"; }
    }

    private void send(WebSocketSession s, String json) {
        try {
            synchronized (s) {
                if (s.isOpen()) s.sendMessage(new TextMessage(json));
            }
        } catch (Exception e) {
            log.debug("WebSocket send failed: {}", e.getMessage());
        }
    }

    /** Every 30s: close sockets that stopped sending heartbeats (user lost internet). */
    @Scheduled(fixedDelay = 30_000)
    public void closeStale() {
        long now = System.currentTimeMillis();
        for (Set<WebSocketSession> set : sessions.values()) {
            for (WebSocketSession s : set) {
                Long t = lastPing.get(s.getId());
                if (t != null && now - t > STALE_MS) {
                    try { s.close(CloseStatus.SESSION_NOT_RELIABLE); } catch (Exception ignored) { }
                }
            }
        }
    }
}
