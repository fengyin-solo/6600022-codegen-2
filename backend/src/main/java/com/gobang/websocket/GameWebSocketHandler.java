package com.gobang.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gobang.model.GameState;
import com.gobang.model.Room;
import com.gobang.service.RoomService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class GameWebSocketHandler implements WebSocketHandler {

    @Autowired
    private RoomService roomService;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    private final Map<String, String> sessionPlayerMap = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String playerId = UUID.randomUUID().toString();
        sessions.put(playerId, session);
        sessionPlayerMap.put(session.getId(), playerId);

        Map<String, Object> response = new HashMap<>();
        response.put("type", "connected");
        response.put("playerId", playerId);
        sendMessage(session, response);
    }

    @Override
    public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) throws Exception {
        String playerId = sessionPlayerMap.get(session.getId());
        if (playerId == null) return;

        String payload = message.getPayload().toString();
        Map<String, Object> data = objectMapper.readValue(payload, Map.class);
        String type = (String) data.get("type");

        switch (type) {
            case "create_room":
                handleCreateRoom(playerId, data);
                break;
            case "join_room":
                handleJoinRoom(playerId, data);
                break;
            case "leave_room":
                handleLeaveRoom(playerId);
                break;
            case "make_move":
                handleMakeMove(playerId, data);
                break;
            case "restart_game":
                handleRestartGame(playerId);
                break;
            case "chat":
                handleChat(playerId, data);
                break;
        }
    }

    private void handleCreateRoom(String playerId, Map<String, Object> data) {
        String playerName = (String) data.getOrDefault("playerName", "玩家");
        Room room = roomService.createRoom(playerId, playerName);
        sendRoomState(room);
    }

    private void handleJoinRoom(String playerId, Map<String, Object> data) {
        String roomCode = ((String) data.getOrDefault("roomCode", "")).toUpperCase();
        String playerName = (String) data.getOrDefault("playerName", "玩家");

        Room room = roomService.joinRoom(roomCode, playerId, playerName);
        if (room == null) {
            sendError(playerId, "房间不存在或已满");
            return;
        }
        sendRoomState(room);
    }

    private void handleLeaveRoom(String playerId) {
        Room room = roomService.getRoomByPlayer(playerId);
        if (room != null) {
            String opponentId = room.getOpponentId(playerId);
            roomService.leaveRoom(playerId);

            if (opponentId != null) {
                Map<String, Object> response = new HashMap<>();
                response.put("type", "opponent_left");
                sendMessage(opponentId, response);

                Room updatedRoom = roomService.getRoom(room.getRoomCode());
                if (updatedRoom != null) {
                    sendRoomState(updatedRoom);
                }
            }
        }
    }

    private void handleMakeMove(String playerId, Map<String, Object> data) {
        Room room = roomService.getRoomByPlayer(playerId);
        if (room == null) return;

        GameState gameState = room.getGameState();
        if (gameState.getWinner() != null) return;

        int playerColor = room.getPlayerColor(playerId);
        if (playerColor != gameState.getCurrentPlayer()) return;

        int row = (int) data.get("row");
        int col = (int) data.get("col");

        if (!gameState.placeStone(row, col)) {
            sendError(playerId, "无效的落子位置");
            return;
        }

        broadcastGameState(room);
    }

    private void handleRestartGame(String playerId) {
        Room room = roomService.getRoomByPlayer(playerId);
        if (room == null) return;

        room.resetGame();
        broadcastGameState(room);
    }

    private void handleChat(String playerId, Map<String, Object> data) {
        Room room = roomService.getRoomByPlayer(playerId);
        if (room == null) return;

        String content = (String) data.get("message");
        String playerName = room.getHostId() != null && room.getHostId().equals(playerId)
                ? room.getHostName() : room.getGuestName();

        Map<String, Object> response = new HashMap<>();
        response.put("type", "chat");
        response.put("playerName", playerName);
        response.put("message", content);
        response.put("timestamp", System.currentTimeMillis());

        broadcastToRoom(room, response);
    }

    private void sendRoomState(Room room) {
        Map<String, Object> hostData = buildRoomStateResponse(room, room.getHostId());
        Map<String, Object> guestData = buildRoomStateResponse(room, room.getGuestId());

        if (room.getHostId() != null) {
            sendMessage(room.getHostId(), hostData);
        }
        if (room.getGuestId() != null) {
            sendMessage(room.getGuestId(), guestData);
        }
    }

    private Map<String, Object> buildRoomStateResponse(Room room, String playerId) {
        Map<String, Object> response = new HashMap<>();
        response.put("type", "room_state");
        response.put("roomCode", room.getRoomCode());
        response.put("hostName", room.getHostName());
        response.put("guestName", room.getGuestName());
        response.put("gameStarted", room.isGameStarted());
        response.put("playerColor", room.getPlayerColor(playerId));
        response.put("isHost", room.getHostId() != null && room.getHostId().equals(playerId));
        response.put("hostColor", room.getHostColor());
        response.put("guestColor", room.getGuestColor());

        GameState gameState = room.getGameState();
        response.put("board", gameState.getBoard());
        response.put("currentPlayer", gameState.getCurrentPlayer());
        response.put("moves", gameState.getMoves());
        response.put("winner", gameState.getWinner());

        return response;
    }

    private void broadcastGameState(Room room) {
        Map<String, Object> hostData = buildRoomStateResponse(room, room.getHostId());
        Map<String, Object> guestData = buildRoomStateResponse(room, room.getGuestId());

        if (room.getHostId() != null) {
            sendMessage(room.getHostId(), hostData);
        }
        if (room.getGuestId() != null) {
            sendMessage(room.getGuestId(), guestData);
        }
    }

    private void broadcastToRoom(Room room, Map<String, Object> message) {
        if (room.getHostId() != null) {
            sendMessage(room.getHostId(), message);
        }
        if (room.getGuestId() != null) {
            sendMessage(room.getGuestId(), message);
        }
    }

    private void sendError(String playerId, String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("type", "error");
        response.put("message", message);
        sendMessage(playerId, response);
    }

    private void sendMessage(String playerId, Map<String, Object> data) {
        WebSocketSession session = sessions.get(playerId);
        if (session != null && session.isOpen()) {
            sendMessage(session, data);
        }
    }

    private void sendMessage(WebSocketSession session, Map<String, Object> data) {
        try {
            String json = objectMapper.writeValueAsString(data);
            session.sendMessage(new TextMessage(json));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        cleanupSession(session);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) throws Exception {
        cleanupSession(session);
    }

    private void cleanupSession(WebSocketSession session) {
        String playerId = sessionPlayerMap.remove(session.getId());
        if (playerId != null) {
            sessions.remove(playerId);
            handleLeaveRoom(playerId);
        }
    }

    @Override
    public boolean supportsPartialMessages() {
        return false;
    }
}
