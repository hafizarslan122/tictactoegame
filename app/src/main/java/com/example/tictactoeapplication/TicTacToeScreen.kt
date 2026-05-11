package com.example.tictactoeapplication

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import android.app.Activity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat

@Composable
fun TicTacToeScreen(
    viewModel: GameViewModel,
    modifier: Modifier = Modifier,
    onNavigateBack: () -> Unit = {}
) {
    val state = viewModel.state
    val context = LocalContext.current
    val interstitialAdManager = remember { InterstitialAdManager(context) }

    LaunchedEffect(Unit) {
        interstitialAdManager.loadAd()
    }

    LaunchedEffect(state.shouldShowInterstitial) {
        if (state.shouldShowInterstitial) {
            val activity = context as? Activity
            if (activity != null) {
                interstitialAdManager.showAd(activity) {
                    viewModel.onInterstitialShown()
                }
            } else {
                viewModel.onInterstitialShown()
            }
        }
    }

    // Comprehensive list of permissions required for Nearby Connections
    val permissionsToRequest = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
            arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_ADVERTISE,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.NEARBY_WIFI_DEVICES
            )
        }
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_ADVERTISE,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        }
        else -> {
            arrayOf(
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        }
    }

    var pendingAction by remember { mutableStateOf<GameAction?>(null) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            pendingAction?.let {
                viewModel.onAction(it)
                pendingAction = null
            }
        } else {
            // Permissions denied
            pendingAction = null
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.surface,
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                )
            )
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AnimatedVisibility(
            visible = !state.isGameStarted,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Spacer(modifier = Modifier.height(24.dp))
                Icon(
                    imageVector = Icons.Rounded.Grid3x3,
                    contentDescription = null,
                    modifier = Modifier.size(80.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Tic Tac Toe",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Master the Grid",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(32.dp))
            }
        }

        AnimatedContent(
            targetState = state.isGameStarted,
            transitionSpec = {
                fadeIn(animationSpec = tween(500)) togetherWith fadeOut(animationSpec = tween(500))
            },
            label = "MainContentTransition"
        ) { isStarted ->
            if (isStarted) {
                GameLayout(state, viewModel)
            } else {
                HomeLayout(state, viewModel, permissionsToRequest, launcher)
            }
        }
    }
}

@Composable
fun HomeLayout(
    state: GameState,
    viewModel: GameViewModel,
    permissionsToRequest: Array<String>,
    launcher: androidx.activity.result.ActivityResultLauncher<Array<String>>
) {
    val context = LocalContext.current
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Text(
            text = "Select Game Mode",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Start
        )

        ModeSelectionGrid(state.gameMode) { mode ->
            viewModel.onAction(GameAction.SelectMode(mode))
        }

        if (state.gameMode == GameMode.LOCAL_P2P) {
            NearbyControlsCard(
                status = state.connectionStatus,
                onHost = {
                    val hasPermissions = permissionsToRequest.all {
                        ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
                    }
                    if (hasPermissions) {
                        viewModel.onAction(GameAction.StartNearbyHost)
                    } else {
                        launcher.launch(permissionsToRequest)
                    }
                },
                onJoin = {
                    val hasPermissions = permissionsToRequest.all {
                        ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
                    }
                    if (hasPermissions) {
                        viewModel.onAction(GameAction.StartNearbyJoin)
                    } else {
                        launcher.launch(permissionsToRequest)
                    }
                },
                onDisconnect = { viewModel.onAction(GameAction.DisconnectNearby) }
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = { viewModel.onAction(GameAction.StartGame) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp)
                .height(56.dp),
            enabled = state.gameMode != GameMode.LOCAL_P2P || state.connectionStatus.contains("Connected"),
            shape = RoundedCornerShape(28.dp),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
        ) {
            Icon(Icons.Default.PlayArrow, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (state.gameMode == GameMode.LOCAL_P2P) "START MULTIPLAYER" else "START GAME",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun GameLayout(state: GameState, viewModel: GameViewModel) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(32.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        StatusSection(state)
        Board(state) { index ->
            viewModel.onAction(GameAction.PlayMove(index))
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedButton(
                onClick = { viewModel.onAction(GameAction.ResetGame) },
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Reset")
            }
            
            Button(
                onClick = { /* Navigate back logic if needed, or just stop game */ 
                    viewModel.onAction(GameAction.SelectMode(state.gameMode)) // This resets and stays on home if we change logic, but for now let's just use it to reset
                },
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
            ) {
                Text("Exit")
            }
        }
    }
}

@Composable
fun ModeSelectionGrid(currentMode: GameMode, onModeSelected: (GameMode) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ModeCard(
                title = "2 Players",
                subtitle = "Local Play",
                icon = Icons.Rounded.People,
                isSelected = currentMode == GameMode.TWO_PLAYERS,
                modifier = Modifier.weight(1f),
                onClick = { onModeSelected(GameMode.TWO_PLAYERS) }
            )
            ModeCard(
                title = "Versus AI",
                subtitle = "Challenge Bot",
                icon = Icons.Rounded.SmartToy,
                isSelected = currentMode == GameMode.VS_AI,
                modifier = Modifier.weight(1f),
                onClick = { onModeSelected(GameMode.VS_AI) }
            )
        }
        ModeCard(
            title = "Local Multiplayer",
            subtitle = "Nearby Devices",
            icon = Icons.Rounded.Wifi,
            isSelected = currentMode == GameMode.LOCAL_P2P,
            modifier = Modifier.fillMaxWidth(),
            onClick = { onModeSelected(GameMode.LOCAL_P2P) }
        )
    }
}

@Composable
fun ModeCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(100.dp),
        shape = RoundedCornerShape(20.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
        ),
        tonalElevation = if (isSelected) 8.dp else 0.dp
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary
                )
            }
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun NearbyControlsCard(
    status: String,
    onHost: () -> Unit,
    onJoin: () -> Unit,
    onDisconnect: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(16.dp))
                Text(text = "Status: $status", style = MaterialTheme.typography.bodyMedium)
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onHost,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Host")
                }
                Button(
                    onClick = onJoin,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Join")
                }
                FilledIconButton(
                    onClick = onDisconnect,
                    shape = RoundedCornerShape(12.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Disconnect", tint = MaterialTheme.colorScheme.error)
                }
            }
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
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(
                        brush = Brush.linearGradient(
                            colors = if (player == Player.X) 
                                listOf(Color(0xFFFF5252), Color(0xFFFF1744)) 
                            else 
                                listOf(Color(0xFF40C4FF), Color(0xFF00B0FF))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = player?.name ?: "",
                    fontSize = 40.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
            }
        }
    }
}
