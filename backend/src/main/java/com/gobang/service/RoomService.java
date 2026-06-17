package com.gobang.service;

import com.gobang.model.Room;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RoomService {

    private final Map<String, Room> rooms = new ConcurrentHashMap<>();
    private final Map<String, String> playerRoomMap = new ConcurrentHashMap<>();
    private final SecureRandom random = new SecureRandom();

    private static final String ROOM_CODE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int ROOM_CODE_LENGTH = 6;

    public Room createRoom(String hostId, String hostName) {
        String roomCode = generateRoomCode();
        Room room = new Room(roomCode, hostId, hostName);
        rooms.put(roomCode, room);
        playerRoomMap.put(hostId, roomCode);
        return room;
    }

    public Room joinRoom(String roomCode, String playerId, String playerName) {
        Room room = rooms.get(roomCode);
        if (room == null) {
            return null;
        }
        if (room.isFull()) {
            return null;
        }
        if (room.getHostId() != null && room.getHostId().equals(playerId)) {
            return room;
        }
        room.setGuestId(playerId);
        room.setGuestName(playerName);
        room.setGameStarted(true);
        playerRoomMap.put(playerId, roomCode);
        return room;
    }

    public void leaveRoom(String playerId) {
        String roomCode = playerRoomMap.get(playerId);
        if (roomCode == null) return;

        Room room = rooms.get(roomCode);
        if (room == null) {
            playerRoomMap.remove(playerId);
            return;
        }

        room.removePlayer(playerId);
        playerRoomMap.remove(playerId);

        if (room.isEmpty()) {
            rooms.remove(roomCode);
        }
    }

    public Room getRoom(String roomCode) {
        return rooms.get(roomCode);
    }

    public Room getRoomByPlayer(String playerId) {
        String roomCode = playerRoomMap.get(playerId);
        if (roomCode == null) return null;
        return rooms.get(roomCode);
    }

    public String getPlayerRoomCode(String playerId) {
        return playerRoomMap.get(playerId);
    }

    private String generateRoomCode() {
        StringBuilder sb;
        do {
            sb = new StringBuilder();
            for (int i = 0; i < ROOM_CODE_LENGTH; i++) {
                sb.append(ROOM_CODE_CHARS.charAt(random.nextInt(ROOM_CODE_CHARS.length())));
            }
        } while (rooms.containsKey(sb.toString()));
        return sb.toString();
    }
}
