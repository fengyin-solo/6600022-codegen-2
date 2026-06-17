package com.gobang.controller;

import com.gobang.model.Room;
import com.gobang.service.RoomService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/room")
@CrossOrigin(origins = "*")
public class RoomController {

    @Autowired
    private RoomService roomService;

    @PostMapping("/create")
    public ResponseEntity<Map<String, Object>> createRoom(@RequestBody Map<String, String> body) {
        String playerId = body.get("playerId");
        String playerName = body.getOrDefault("playerName", "玩家");

        if (playerId == null || playerId.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "playerId is required"));
        }

        Room existingRoom = roomService.getRoomByPlayer(playerId);
        if (existingRoom != null) {
            return ResponseEntity.badRequest().body(Map.of("error", "玩家已在房间中"));
        }

        Room room = roomService.createRoom(playerId, playerName);
        return ResponseEntity.ok(buildRoomResponse(room));
    }

    @PostMapping("/join")
    public ResponseEntity<Map<String, Object>> joinRoom(@RequestBody Map<String, String> body) {
        String roomCode = body.getOrDefault("roomCode", "").toUpperCase();
        String playerId = body.get("playerId");
        String playerName = body.getOrDefault("playerName", "玩家");

        if (playerId == null || playerId.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "playerId is required"));
        }
        if (roomCode == null || roomCode.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "roomCode is required"));
        }

        Room existingRoom = roomService.getRoomByPlayer(playerId);
        if (existingRoom != null && !existingRoom.getRoomCode().equals(roomCode)) {
            return ResponseEntity.badRequest().body(Map.of("error", "玩家已在其他房间中"));
        }

        Room room = roomService.joinRoom(roomCode, playerId, playerName);
        if (room == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "房间不存在或已满"));
        }

        return ResponseEntity.ok(buildRoomResponse(room));
    }

    @PostMapping("/leave")
    public ResponseEntity<Map<String, Object>> leaveRoom(@RequestBody Map<String, String> body) {
        String playerId = body.get("playerId");
        if (playerId == null || playerId.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "playerId is required"));
        }

        roomService.leaveRoom(playerId);
        return ResponseEntity.ok(Map.of("success", true));
    }

    @GetMapping("/{roomCode}")
    public ResponseEntity<Map<String, Object>> getRoom(@PathVariable String roomCode) {
        Room room = roomService.getRoom(roomCode.toUpperCase());
        if (room == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(buildRoomResponse(room));
    }

    private Map<String, Object> buildRoomResponse(Room room) {
        Map<String, Object> response = new HashMap<>();
        response.put("roomCode", room.getRoomCode());
        response.put("hostName", room.getHostName());
        response.put("guestName", room.getGuestName());
        response.put("isFull", room.isFull());
        response.put("gameStarted", room.isGameStarted());
        response.put("hostColor", room.getHostColor());
        response.put("guestColor", room.getGuestColor());
        response.put("board", room.getGameState().getBoard());
        response.put("currentPlayer", room.getGameState().getCurrentPlayer());
        response.put("moves", room.getGameState().getMoves());
        response.put("winner", room.getGameState().getWinner());
        return response;
    }
}
