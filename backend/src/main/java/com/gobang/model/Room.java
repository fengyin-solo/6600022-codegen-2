package com.gobang.model;

import lombok.Data;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Data
public class Room {
    private String roomCode;
    private String hostId;
    private String guestId;
    private GameState gameState;
    private String hostName;
    private String guestName;
    private int hostColor;
    private int guestColor;
    private long createdAt;
    private volatile boolean gameStarted;

    public Room(String roomCode, String hostId, String hostName) {
        this.roomCode = roomCode;
        this.hostId = hostId;
        this.hostName = hostName;
        this.gameState = new GameState(roomCode);
        this.hostColor = GameState.BLACK;
        this.guestColor = GameState.WHITE;
        this.createdAt = System.currentTimeMillis();
        this.gameStarted = false;
    }

    public boolean isFull() {
        return guestId != null;
    }

    public boolean isEmpty() {
        return guestId == null && hostId == null;
    }

    public void removePlayer(String playerId) {
        if (hostId != null && hostId.equals(playerId)) {
            hostId = null;
            hostName = null;
        } else if (guestId != null && guestId.equals(playerId)) {
            guestId = null;
            guestName = null;
        }
    }

    public String getOpponentId(String playerId) {
        if (hostId != null && hostId.equals(playerId)) {
            return guestId;
        } else if (guestId != null && guestId.equals(playerId)) {
            return hostId;
        }
        return null;
    }

    public int getPlayerColor(String playerId) {
        if (hostId != null && hostId.equals(playerId)) {
            return hostColor;
        } else if (guestId != null && guestId.equals(playerId)) {
            return guestColor;
        }
        return 0;
    }

    public void resetGame() {
        this.gameState = new GameState(roomCode);
        this.gameStarted = false;
    }
}
