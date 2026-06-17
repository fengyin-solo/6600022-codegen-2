<template>
  <div class="bg-gray-900 rounded-xl p-4 border border-gray-700">
    <h3 class="text-lg font-bold text-green-400 mb-3">双人对战</h3>

    <div v-if="!store.isOnlineMode" class="space-y-3">
      <p class="text-gray-400 text-sm">选择模式开始游戏</p>
      <div class="flex gap-2">
        <button
          @click="store.setGameMode('solo')"
          class="flex-1 py-2 rounded-lg transition-colors text-sm font-medium"
          :class="store.gameMode === 'solo' ? 'bg-green-600 text-white' : 'bg-gray-800 text-gray-400 hover:bg-gray-700'"
        >
          单人模式
        </button>
        <button
          @click="store.setGameMode('online')"
          class="flex-1 py-2 rounded-lg transition-colors text-sm font-medium"
          :class="store.gameMode === 'online' ? 'bg-green-600 text-white' : 'bg-gray-800 text-gray-400 hover:bg-gray-700'"
        >
          双人对战
        </button>
      </div>
    </div>

    <div v-else-if="!store.inRoom" class="space-y-4">
      <div class="flex gap-2 mb-2">
        <button
          @click="store.setGameMode('solo')"
          class="text-xs text-gray-400 hover:text-white transition-colors"
        >
          ← 返回单人模式
        </button>
      </div>

      <div>
        <label class="block text-sm text-gray-400 mb-1">你的昵称</label>
        <input
          v-model="localPlayerName"
          type="text"
          placeholder="输入昵称"
          class="w-full px-3 py-2 bg-gray-800 border border-gray-700 rounded-lg text-white text-sm focus:outline-none focus:border-green-500"
          maxlength="10"
        />
      </div>

      <div class="border-t border-gray-700 pt-4">
        <h4 class="text-sm font-medium text-white mb-3">创建房间</h4>
        <button
          @click="handleCreateRoom"
          :disabled="!localPlayerName.trim()"
          class="w-full py-2 bg-green-600 hover:bg-green-500 disabled:bg-gray-700 disabled:cursor-not-allowed text-white rounded-lg transition-colors text-sm font-medium"
        >
          创建房间
        </button>
      </div>

      <div class="border-t border-gray-700 pt-4">
        <h4 class="text-sm font-medium text-white mb-3">加入房间</h4>
        <div class="space-y-2">
          <input
            v-model="joinRoomCode"
            type="text"
            placeholder="输入房间码"
            class="w-full px-3 py-2 bg-gray-800 border border-gray-700 rounded-lg text-white text-sm focus:outline-none focus:border-green-500 uppercase tracking-widest"
            maxlength="6"
          />
          <button
            @click="handleJoinRoom"
            :disabled="!localPlayerName.trim() || joinRoomCode.length !== 6"
            class="w-full py-2 bg-blue-600 hover:bg-blue-500 disabled:bg-gray-700 disabled:cursor-not-allowed text-white rounded-lg transition-colors text-sm font-medium"
          >
            加入房间
          </button>
        </div>
      </div>

      <div v-if="store.wsError" class="text-red-400 text-sm text-center py-2 bg-red-900/30 rounded-lg">
        {{ store.wsError }}
      </div>
    </div>

    <div v-else class="space-y-4">
      <div class="flex items-center justify-between">
        <div>
          <span class="text-xs text-gray-500">房间码</span>
          <div class="flex items-center gap-2">
            <span class="text-xl font-mono font-bold text-yellow-400 tracking-wider">{{ store.roomState?.roomCode }}</span>
            <button
              @click="copyRoomCode"
              class="p-1 text-gray-400 hover:text-white transition-colors"
              title="复制房间码"
            >
              <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 16H6a2 2 0 01-2-2V6a2 2 0 012-2h8a2 2 0 012 2v2m-6 12h8a2 2 0 002-2v-8a2 2 0 00-2-2h-8a2 2 0 00-2 2v8a2 2 0 002 2z"/>
              </svg>
            </button>
          </div>
        </div>
        <span class="text-xs px-2 py-1 rounded-full" :class="store.wsConnected ? 'bg-green-900 text-green-400' : 'bg-red-900 text-red-400'">
          {{ store.wsConnected ? '已连接' : '连接断开' }}
        </span>
      </div>

      <div class="grid grid-cols-2 gap-2">
        <div class="bg-gray-800 rounded-lg p-3 text-center">
          <div class="flex items-center justify-center gap-1 mb-1">
            <span class="inline-block w-3 h-3 rounded-full" :class="store.roomState?.hostColor === 1 ? 'bg-gray-800 border border-gray-600' : 'bg-white'"></span>
            <span class="text-xs text-gray-400">黑棋</span>
          </div>
          <p class="text-white text-sm font-medium truncate">{{ store.roomState?.hostName || '等待中...' }}</p>
          <p v-if="store.roomState?.isHost" class="text-xs text-green-400 mt-1">(你)</p>
        </div>
        <div class="bg-gray-800 rounded-lg p-3 text-center">
          <div class="flex items-center justify-center gap-1 mb-1">
            <span class="inline-block w-3 h-3 rounded-full" :class="store.roomState?.guestColor === 1 ? 'bg-gray-800 border border-gray-600' : 'bg-white'"></span>
            <span class="text-xs text-gray-400">白棋</span>
          </div>
          <p class="text-white text-sm font-medium truncate">{{ store.roomState?.guestName || '等待中...' }}</p>
          <p v-if="!store.roomState?.isHost && store.roomState?.guestName" class="text-xs text-green-400 mt-1">(你)</p>
        </div>
      </div>

      <div v-if="!store.roomState?.gameStarted" class="text-center py-2 bg-yellow-900/30 rounded-lg">
        <p class="text-yellow-400 text-sm">等待对手加入...</p>
        <p class="text-xs text-gray-400 mt-1">将房间码分享给好友开始对战</p>
      </div>

      <div v-if="store.roomState?.gameStarted && store.roomWinner === null" class="text-center py-2 bg-blue-900/30 rounded-lg">
        <p class="text-sm" :class="store.isMyTurn ? 'text-green-400' : 'text-blue-400'">
          {{ store.isMyTurn ? '轮到你落子' : '等待对手落子' }}
        </p>
      </div>

      <div v-if="store.roomWinner !== null" class="text-center py-3 bg-green-900/30 rounded-lg">
        <p class="text-lg font-bold" :class="store.roomWinner === 0 ? 'text-yellow-400' : store.roomWinner === store.roomState?.playerColor ? 'text-green-400' : 'text-red-400'">
          {{ store.roomWinner === 0 ? '平局！' : store.roomWinner === store.roomState?.playerColor ? '你赢了！' : '你输了...' }}
        </p>
      </div>

      <div class="flex gap-2">
        <button
          v-if="store.roomState?.gameStarted && store.roomWinner !== null"
          @click="store.restartOnlineGame()"
          class="flex-1 py-2 bg-green-600 hover:bg-green-500 text-white rounded-lg transition-colors text-sm font-medium"
        >
          再来一局
        </button>
        <button
          @click="handleLeaveRoom"
          class="flex-1 py-2 bg-red-600/20 border border-red-600/50 text-red-400 hover:bg-red-600/30 rounded-lg transition-colors text-sm"
        >
          离开房间
        </button>
      </div>

      <div v-if="store.wsError" class="text-red-400 text-sm text-center py-2 bg-red-900/30 rounded-lg">
        {{ store.wsError }}
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import { useGameStore } from '../store/game';

const store = useGameStore();
const localPlayerName = ref('');
const joinRoomCode = ref('');

function handleCreateRoom() {
  store.createRoom(localPlayerName.value.trim());
}

function handleJoinRoom() {
  store.joinRoom(joinRoomCode.value.trim(), localPlayerName.value.trim());
}

function handleLeaveRoom() {
  store.leaveRoom();
}

function copyRoomCode() {
  if (store.roomState?.roomCode) {
    navigator.clipboard.writeText(store.roomState.roomCode);
  }
}
</script>
