export type BoardState = number[][];

export interface Move {
  row: number;
  col: number;
  player: number;
  timestamp: number;
}

export interface GameRecord {
  id: string;
  moves: Move[];
  winner: number | null;
  createdAt: string;
  duration: number;
}

export interface AIConfig {
  depth: number;
  enabled: boolean;
  playerColor: number;
}

export type GameStatus = 'idle' | 'playing' | 'finished' | 'replaying';

export type GameMode = 'solo' | 'online';

export interface RoomState {
  roomCode: string;
  hostName: string;
  guestName: string | null;
  gameStarted: boolean;
  playerColor: number;
  isHost: boolean;
  hostColor: number;
  guestColor: number;
  board: BoardState;
  currentPlayer: number;
  moves: Move[];
  winner: number | null;
}

export interface ChatMessage {
  playerName: string;
  message: string;
  timestamp: number;
}

export type WsMessageType =
  | 'connected'
  | 'room_state'
  | 'opponent_left'
  | 'error'
  | 'chat';

export interface WsMessage {
  type: WsMessageType;
  playerId?: string;
  roomCode?: string;
  hostName?: string;
  guestName?: string | null;
  gameStarted?: boolean;
  playerColor?: number;
  isHost?: boolean;
  board?: BoardState;
  currentPlayer?: number;
  moves?: Move[];
  winner?: number | null;
  message?: string;
  playerName?: string;
  timestamp?: number;
}
