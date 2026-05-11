package com.example.tictactoeapplication

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tictactoeapplication.multiplayer.ConnectionManager
import com.example.tictactoeapplication.multiplayer.GameMove
import com.example.tictactoeapplication.multiplayer.MessageType
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class GameViewModel(private val connectionManager: ConnectionManager) : ViewModel() {
    var state by mutableStateOf(GameState())
        private set

    private var gamesPlayedCount = 0

    init {
        observeConnectionState()
    }

    private fun observeConnectionState() {
        connectionManager.connectionState
            .onEach { connectionState ->
                when (connectionState) {
                    is ConnectionManager.ConnectionState.Connected -> {
                        state = state.copy(
                            connectionStatus = "Connected to ${connectionState.endpointName}",
                            isGameStarted = true
                        )
                    }
                    is ConnectionManager.ConnectionState.PayloadReceived -> {
                        handleRemoteMove(connectionState.move)
                    }
                    is ConnectionManager.ConnectionState.Error -> {
                        state = state.copy(connectionStatus = connectionState.message)
                    }
                    ConnectionManager.ConnectionState.Advertising -> {
                        state = state.copy(connectionStatus = "Advertising...")
                    }
                    ConnectionManager.ConnectionState.Discovering -> {
                        state = state.copy(connectionStatus = "Discovering...")
                    }
                    ConnectionManager.ConnectionState.Idle -> {
                        state = state.copy(connectionStatus = "Idle")
                    }
                }
            }
            .launchIn(viewModelScope)
    }

    fun onAction(action: GameAction) {
        when (action) {
            is GameAction.PlayMove -> playMove(action.index)
            GameAction.ResetGame -> {
                resetGame(keepStarted = true)
                if (state.gameMode == GameMode.LOCAL_P2P) {
                    connectionManager.sendMove(GameMove(type = MessageType.RESET))
                }
            }
            is GameAction.SelectMode -> {
                state = state.copy(gameMode = action.mode, localPlayer = null)
                resetGame(keepStarted = false)
            }
            GameAction.StartGame -> {
                state = state.copy(isGameStarted = true)
                if (state.gameMode == GameMode.LOCAL_P2P) {
                    connectionManager.sendMove(GameMove(type = MessageType.START))
                }
            }
            is GameAction.ToggleDarkMode -> {
                state = state.copy(isDarkMode = action.enabled)
            }
            is GameAction.StartNearbyHost -> {
                state = state.copy(localPlayer = Player.X)
                connectionManager.startAdvertising("Host")
            }
            is GameAction.StartNearbyJoin -> {
                state = state.copy(localPlayer = Player.O)
                connectionManager.startDiscovery()
            }
            GameAction.DisconnectNearby -> {
                connectionManager.disconnect()
                state = state.copy(localPlayer = null)
            }
        }
    }

    private fun handleRemoteMove(move: GameMove) {
        when (move.type) {
            MessageType.START -> {
                state = state.copy(isGameStarted = true)
            }
            MessageType.RESET -> {
                resetGame(keepStarted = true)
            }
            MessageType.MOVE -> {
                // Apply move if it's currently that player's turn according to local state
                if (state.currentPlayer.name == move.player) {
                    applyMove(move.index)
                }
            }
        }
    }

    private fun playMove(index: Int) {
        if (!state.isGameStarted || state.board[index] != null || state.winner != null || state.isDraw) return
        
        // In P2P mode, only allow moves if it's the local player's turn
        if (state.gameMode == GameMode.LOCAL_P2P && state.localPlayer != null) {
            if (state.currentPlayer != state.localPlayer) return
        }
        
        if (state.gameMode == GameMode.LOCAL_P2P) {
            connectionManager.sendMove(GameMove(index, state.currentPlayer.name, MessageType.MOVE))
        }

        applyMove(index)

        if (state.gameMode == GameMode.VS_AI && !state.isDraw && state.winner == null && state.currentPlayer == Player.O) {
            makeAiMove()
        }
    }

    private fun applyMove(index: Int) {
        val newBoard = state.board.toMutableList()
        newBoard[index] = state.currentPlayer

        val winner = checkWinner(newBoard)
        val isDraw = winner == null && newBoard.all { it != null }

        state = state.copy(
            board = newBoard,
            currentPlayer = if (state.currentPlayer == Player.X) Player.O else Player.X,
            winner = winner,
            isDraw = isDraw
        )

        if (winner != null || isDraw) {
            gamesPlayedCount++
            if (gamesPlayedCount % 3 == 0) {
                state = state.copy(shouldShowInterstitial = true)
            }
        }
    }

    fun onInterstitialShown() {
        state = state.copy(shouldShowInterstitial = false)
    }

    override fun onCleared() {
        super.onCleared()
        connectionManager.disconnect()
    }

    private fun makeAiMove() {
        viewModelScope.launch {
            state = state.copy(isAiThinking = true)
            delay(600) // Small delay to feel natural

            val emptyIndices = state.board.indices.filter { state.board[it] == null }
            if (emptyIndices.isNotEmpty()) {
                val bestMove = findBestMove(state.board) ?: emptyIndices.random()

                val aiBoard = state.board.toMutableList()
                aiBoard[bestMove] = Player.O
                val winner = checkWinner(aiBoard)
                val isDraw = winner == null && aiBoard.all { it != null }

                state = state.copy(
                    board = aiBoard,
                    currentPlayer = Player.X,
                    winner = winner,
                    isDraw = isDraw,
                    isAiThinking = false
                )
            } else {
                state = state.copy(isAiThinking = false)
            }
        }
    }

    private fun findBestMove(board: List<Player?>): Int? {
        // 1. Try to win
        for (i in board.indices) {
            if (board[i] == null) {
                val testBoard = board.toMutableList()
                testBoard[i] = Player.O
                if (checkWinner(testBoard) == Player.O) return i
            }
        }
        // 2. Block player
        for (i in board.indices) {
            if (board[i] == null) {
                val testBoard = board.toMutableList()
                testBoard[i] = Player.X
                if (checkWinner(testBoard) == Player.X) return i
            }
        }
        return null
    }

    private fun resetGame(keepStarted: Boolean = false) {
        state = state.copy(
            board = List(9) { null },
            currentPlayer = Player.X,
            winner = null,
            isDraw = false,
            isAiThinking = false,
            isGameStarted = keepStarted
        )
    }

    private fun checkWinner(board: List<Player?>): Player? {
        val winPatterns = listOf(
            listOf(0, 1, 2), listOf(3, 4, 5), listOf(6, 7, 8), // Rows
            listOf(0, 3, 6), listOf(1, 4, 7), listOf(2, 5, 8), // Cols
            listOf(0, 4, 8), listOf(2, 4, 6) // Diagonals
        )

        for (pattern in winPatterns) {
            if (board[pattern[0]] != null &&
                board[pattern[0]] == board[pattern[1]] &&
                board[pattern[0]] == board[pattern[2]]
            ) {
                return board[pattern[0]]
            }
        }
        return null
    }
}

data class GameState(
    val board: List<Player?> = List(9) { null },
    val currentPlayer: Player = Player.X,
    val winner: Player? = null,
    val isDraw: Boolean = false,
    val gameMode: GameMode = GameMode.TWO_PLAYERS,
    val isAiThinking: Boolean = false,
    val isGameStarted: Boolean = false,
    val isDarkMode: Boolean = false,
    val connectionStatus: String = "Idle",
    val localPlayer: Player? = null,
    val shouldShowInterstitial: Boolean = false
)

enum class Player { X, O }
enum class GameMode { TWO_PLAYERS, VS_AI, LOCAL_P2P }

sealed class GameAction {
    data class PlayMove(val index: Int) : GameAction()
    object ResetGame : GameAction()
    data class SelectMode(val mode: GameMode) : GameAction()
    object StartGame : GameAction()
    data class ToggleDarkMode(val enabled: Boolean) : GameAction()
    object StartNearbyHost : GameAction()
    object StartNearbyJoin : GameAction()
    object DisconnectNearby : GameAction()
}
