package com.example.tictactoeapplication

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class GameViewModel : ViewModel() {
    var state by mutableStateOf(GameState())
        private set

    fun onAction(action: GameAction) {
        when (action) {
            is GameAction.PlayMove -> playMove(action.index)
            GameAction.ResetGame -> resetGame()
            is GameAction.SelectMode -> {
                state = state.copy(gameMode = action.mode)
                resetGame()
            }
            GameAction.StartGame -> {
                state = state.copy(isGameStarted = true)
            }
            is GameAction.ToggleDarkMode -> {
                state = state.copy(isDarkMode = action.enabled)
            }
        }
    }

    private fun playMove(index: Int) {
        if (!state.isGameStarted || state.board[index] != null || state.winner != null || state.isDraw) return
        if (state.currentPlayer == Player.O && state.gameMode == GameMode.VS_AI && !state.isAiThinking) {
            // If it's AI's turn but playMove was called from UI, ignore it
            return
        }

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

        if (state.gameMode == GameMode.VS_AI && !isDraw && winner == null && state.currentPlayer == Player.O) {
            makeAiMove()
        }
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

    private fun resetGame() {
        state = state.copy(
            board = List(9) { null },
            currentPlayer = Player.X,
            winner = null,
            isDraw = false,
            isAiThinking = false,
            isGameStarted = false
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
    val isDarkMode: Boolean = false
)

enum class Player { X, O }
enum class GameMode { TWO_PLAYERS, VS_AI }

sealed class GameAction {
    data class PlayMove(val index: Int) : GameAction()
    object ResetGame : GameAction()
    data class SelectMode(val mode: GameMode) : GameAction()
    object StartGame : GameAction()
    data class ToggleDarkMode(val enabled: Boolean) : GameAction()
}
