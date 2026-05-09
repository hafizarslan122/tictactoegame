package com.example.tictactoeapplication

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun TicTacToeScreen(
    viewModel: GameViewModel,
    modifier: Modifier = Modifier,
    onNavigateBack: () -> Unit = {}
) {
    val state = viewModel.state

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceEvenly
    ) {
        Text(
            text = "Tic Tac Toe",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        ModeSelectionSection(state.gameMode) { mode ->
            viewModel.onAction(GameAction.SelectMode(mode))
        }

        AnimatedContent(
            targetState = state.isGameStarted,
            transitionSpec = {
                (fadeIn(animationSpec = tween(500, delayMillis = 90)) + 
                 scaleIn(initialScale = 0.8f, animationSpec = tween(500, delayMillis = 90)))
                    .togetherWith(fadeOut(animationSpec = tween(300)) + scaleOut(targetScale = 0.8f, animationSpec = tween(300)))
            },
            label = "GameContentTransition"
        ) { isStarted ->
            if (isStarted) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    StatusSection(state)
                    Board(state) { index ->
                        viewModel.onAction(GameAction.PlayMove(index))
                    }
                    Button(
                        onClick = { viewModel.onAction(GameAction.ResetGame) },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .height(56.dp)
                            .fillMaxWidth(0.5f)
                    ) {
                        Text("Reset Game", fontSize = 18.sp)
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(400.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Button(
                        onClick = { viewModel.onAction(GameAction.StartGame) },
                        modifier = Modifier
                            .size(160.dp),
                        shape = RoundedCornerShape(80.dp),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp)
                            )
                            Text("START", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ModeSelectionSection(currentMode: GameMode, onModeSelected: (GameMode) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedButton(
            onClick = { onModeSelected(GameMode.TWO_PLAYERS) },
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(12.dp),
            colors = if (currentMode == GameMode.TWO_PLAYERS) {
                ButtonDefaults.outlinedButtonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            } else ButtonDefaults.outlinedButtonColors()
        ) {
            Text("2 Players")
        }

        OutlinedButton(
            onClick = { onModeSelected(GameMode.VS_AI) },
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(12.dp),
            colors = if (currentMode == GameMode.VS_AI) {
                ButtonDefaults.outlinedButtonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            } else ButtonDefaults.outlinedButtonColors()
        ) {
            Text("Play with AI")
        }
    }
}

@Composable
fun StatusSection(state: GameState) {
    val statusText = when {
        state.isAiThinking -> "AI is thinking..."
        state.winner != null -> {
            if (state.gameMode == GameMode.VS_AI) {
                if (state.winner == Player.O) "AI Wins!" else "You Win!"
            } else "Player ${state.winner} Wins!"
        }
        state.isDraw -> "It's a Draw!"
        else -> {
            if (state.gameMode == GameMode.VS_AI) {
                if (state.currentPlayer == Player.X) "Your Turn" else "AI's Turn"
            } else "Player ${state.currentPlayer}'s Turn"
        }
    }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Text(
            text = statusText,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSecondaryContainer
        )
    }
}

@Composable
fun Board(state: GameState, onCellClick: (Int) -> Unit) {
    Column(
        modifier = Modifier
            .aspectRatio(1f)
            .padding(8.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(16.dp))
            .padding(8.dp)
    ) {
        for (row in 0..2) {
            Row(modifier = Modifier.weight(1f)) {
                for (col in 0..2) {
                    val index = row * 3 + col
                    Cell(
                        player = state.board[index],
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .padding(4.dp),
                        onClick = { onCellClick(index) }
                    )
                }
            }
        }
    }
}

@Composable
fun Cell(player: Player?, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        AnimatedVisibility(
            visible = player != null,
            enter = fadeIn(tween(300)) + scaleIn(tween(300))
        ) {
            val color = if (player == Player.X) Color(0xFFE91E63) else Color(0xFF2196F3)
            Text(
                text = player?.name ?: "",
                fontSize = 48.sp,
                fontWeight = FontWeight.ExtraBold,
                color = color
            )
        }
    }
}
