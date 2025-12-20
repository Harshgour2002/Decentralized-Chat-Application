package com.example.Decentralized_Chat_Application.signalling;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import jakarta.annotation.PostConstruct;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.socket.*;

@Component
public class SignallingServer extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(SignallingServer.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    /* userId -> WebSocketSession */
    private final Map<String, WebSocketSession> onlineUsers = new ConcurrentHashMap<>();

    /* sessionId -> userId */
    private final Map<String, String> sessionToUser = new ConcurrentHashMap<>();

    /* userId -> friendIds */
    private final Map<String, Set<String>> friendsMap = new ConcurrentHashMap<>();

    /* ---------------- MOCK FRIEND DATA ---------------- */
    @PostConstruct
    private void loadFriends() {
        friendsMap.put("user1", Set.of("user2", "user3", "user4"));
        friendsMap.put("user2", Set.of("user1", "user3", "user5"));
        friendsMap.put("user3", Set.of("user1", "user2"));
        friendsMap.put("user4", Set.of("user1", "user5"));
        friendsMap.put("user5", Set.of("user2", "user4"));
    }

    /* ---------------- REST HELPERS ---------------- */
    public List<String> getOnlineUsers() {
        return new ArrayList<>(onlineUsers.keySet());
    }

    public Map<String, Object> getUsersStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("onlineCount", onlineUsers.size());
        status.put("timestamp", System.currentTimeMillis());
        return status;
    }


    public List<String> getFriends(String userId) {
        return new ArrayList<>(friendsMap.getOrDefault(userId, Set.of()));
    }

    /* ---------------- WEBSOCKET LIFECYCLE ---------------- */

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        ObjectNode hello = MAPPER.createObjectNode();
        hello.put("type", "hello");
        hello.put("message", "Send {type:'register', userId:'YOUR_ID'}");
        session.sendMessage(new TextMessage(hello.toString()));
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {

        JsonNode json;
        try {
            json = MAPPER.readTree(message.getPayload());
        } catch (Exception e) {
            log.warn("Invalid JSON: {}", message.getPayload());
            return;
        }

        String type = json.path("type").asText();
        if (type.isEmpty()) return;

        switch (type) {
            case "register" -> register(session, json);
            case "offer", "answer", "ice" -> relay(session, json);
            case "get-online-friends" -> sendOnlineFriends(session);
            default -> log.warn("Unknown type: {}", type);
        }
    }

    /* ---------------- REGISTER ---------------- */

    private void register(WebSocketSession session, JsonNode json) throws Exception {

        String userId = json.path("userId").asText();
        if (userId.isEmpty()) return;

        onlineUsers.put(userId, session);
        sessionToUser.put(session.getId(), userId);

        ObjectNode ok = MAPPER.createObjectNode();
        ok.put("type", "registered");
        ok.put("userId", userId);
        session.sendMessage(new TextMessage(ok.toString()));

        notifyFriendsPresence(userId, true);
        sendOnlineFriends(session);
    }

    /* ---------------- FRIEND PRESENCE ---------------- */

    private void sendOnlineFriends(WebSocketSession session) throws Exception {

        String userId = sessionToUser.get(session.getId());
        if (userId == null) return;

        Set<String> friends = friendsMap.getOrDefault(userId, Set.of());
        ArrayNode onlineFriends = MAPPER.createArrayNode();

        for (String f : friends) {
            if (onlineUsers.containsKey(f)) {
                onlineFriends.add(f);
            }
        }

        ObjectNode msg = MAPPER.createObjectNode();
        msg.put("type", "online-friends");
        msg.set("friends", onlineFriends);

        session.sendMessage(new TextMessage(msg.toString()));
    }

    private void notifyFriendsPresence(String userId, boolean online) {

        Set<String> friends = friendsMap.getOrDefault(userId, Set.of());

        ObjectNode msg = MAPPER.createObjectNode();
        msg.put("type", "friend-status");
        msg.put("userId", userId);
        msg.put("online", online);

        friends.forEach(f -> {
            WebSocketSession s = onlineUsers.get(f);
            if (s != null && s.isOpen()) {
                try {
                    s.sendMessage(new TextMessage(msg.toString()));
                } catch (Exception ignored) {}
            }
        });
    }

    /* ---------------- FRIEND-ONLY SIGNAL RELAY ---------------- */

    private void relay(WebSocketSession session, JsonNode json) throws Exception {

        String from = sessionToUser.get(session.getId());
        String to = json.path("to").asText();

        if (from == null || to.isEmpty()) return;

        // 🔐 FRIEND CHECK
        if (!friendsMap.getOrDefault(from, Set.of()).contains(to)) {
            log.warn("Blocked signaling: {} -> {}", from, to);
            return;
        }

        WebSocketSession target = onlineUsers.get(to);
        if (target == null || !target.isOpen()) return;

        ObjectNode payload = (ObjectNode) json;
        payload.put("from", from);

        target.sendMessage(new TextMessage(payload.toString()));
    }

    /* ---------------- DISCONNECT ---------------- */

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {

        String userId = sessionToUser.remove(session.getId());
        if (userId == null) return;

        onlineUsers.remove(userId);
        notifyFriendsPresence(userId, false);
    }
}               