package com.example.tictactoeapplication

import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

sealed class Screen(val route: String) {
    object Game : Screen("game")
    object Settings : Screen("settings")
    object Share : Screen("share")
    object PrivacyPolicy : Screen("privacy_policy")
    object About : Screen("about")
    object HowToPlay : Screen("how_to_play")
}

@Composable
fun AppNavigation(viewModel: GameViewModel) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Screen.Game.route) {
        composable(Screen.Game.route) {
            GameScreenContainer(
                viewModel = viewModel,
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) }
            )
        }
        composable(Screen.Settings.route) {
            SettingsScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onNavigateToHowToPlay = { navController.navigate(Screen.HowToPlay.route) },
                onNavigateToAbout = { navController.navigate(Screen.About.route) },
                onNavigateToPrivacy = { navController.navigate(Screen.PrivacyPolicy.route) },
                onNavigateToShare = { navController.navigate(Screen.Share.route) }
            )
        }
        composable(Screen.Share.route) {
            ShareScreen(onBack = { navController.popBackStack() })
        }
        composable(Screen.About.route) {
            AboutScreen(onBack = { navController.popBackStack() })
        }
        composable(Screen.PrivacyPolicy.route) {
            PrivacyPolicyScreen(onBack = { navController.popBackStack() })
        }
        composable(Screen.HowToPlay.route) {
            HowToPlayScreen(onBack = { navController.popBackStack() })
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameScreenContainer(
    viewModel: GameViewModel,
    onNavigateToSettings: () -> Unit
) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("") },
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        }
    ) { innerPadding ->
        TicTacToeScreen(
            viewModel = viewModel,
            modifier = Modifier.padding(innerPadding)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: GameViewModel,
    onBack: () -> Unit,
    onNavigateToHowToPlay: () -> Unit,
    onNavigateToAbout: () -> Unit,
    onNavigateToPrivacy: () -> Unit,
    onNavigateToShare: () -> Unit
) {
    val state = viewModel.state
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                "Appearance",
                modifier = Modifier.padding(16.dp),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )

            ListItem(
                headlineContent = { Text("Dark Mode") },
                supportingContent = {
                    Text(if (state.isDarkMode) "Enabled" else "Disabled")
                },
                leadingContent = {
                    Icon(
                        if (state.isDarkMode) Icons.Default.DarkMode else Icons.Default.LightMode,
                        contentDescription = null
                    )
                },
                trailingContent = {
                    Switch(
                        checked = state.isDarkMode,
                        onCheckedChange = { isChecked ->
                            viewModel.onAction(GameAction.ToggleDarkMode(isChecked))
                        }
                    )
                }
            )

            HorizontalDivider()

            Text(
                "Game Options",
                modifier = Modifier.padding(16.dp),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )

            ListItem(
                headlineContent = { Text("Game Mode") },
                supportingContent = {
                    Text(if (state.gameMode == GameMode.VS_AI) "Playing against AI" else "2 Players Mode")
                },
                trailingContent = {
                    Switch(
                        checked = state.gameMode == GameMode.VS_AI,
                        onCheckedChange = { isChecked ->
                            val mode = if (isChecked) GameMode.VS_AI else GameMode.TWO_PLAYERS
                            viewModel.onAction(GameAction.SelectMode(mode))
                        }
                    )
                }
            )

            HorizontalDivider()

            Text(
                "Information",
                modifier = Modifier.padding(16.dp),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )

            SettingsItem(
                icon = Icons.AutoMirrored.Filled.Help,
                title = "How to Play",
                onClick = onNavigateToHowToPlay
            )
            SettingsItem(
                icon = Icons.Default.Info,
                title = "About",
                onClick = onNavigateToAbout
            )
            SettingsItem(
                icon = Icons.Default.Share,
                title = "Share App",
                onClick = onNavigateToShare
            )
            SettingsItem(
                icon = Icons.Default.Lock,
                title = "Privacy Policy",
                onClick = onNavigateToPrivacy
            )
        }
    }
}

@Composable
fun SettingsItem(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, onClick: () -> Unit) {
    ListItem(
        modifier = Modifier.clickable(onClick = onClick),
        leadingContent = { Icon(icon, contentDescription = null) },
        headlineContent = { Text(title) },
        trailingContent = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null) }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HowToPlayScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("How to Play") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                "Game Objective",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "The goal of Tic Tac Toe is to be the first player to get three of your marks in a row (horizontally, vertically, or diagonally) on a 3x3 grid.",
                style = MaterialTheme.typography.bodyLarge
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                "Rules",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            val rules = listOf(
                "The game is played on a grid that's 3 squares by 3 squares.",
                "You are 'X', your friend (or the AI) is 'O'. Players take turns putting their marks in empty squares.",
                "The first player to get 3 of her marks in a row (up, down, across, or diagonally) is the winner.",
                "When all 9 squares are full, the game is over. If no player has 3 in a row, the game ends in a tie (Draw)."
            )
            
            rules.forEachIndexed { index, rule ->
                Row(modifier = Modifier.padding(vertical = 4.dp)) {
                    Text("${index + 1}. ", fontWeight = FontWeight.Bold)
                    Text(rule)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                "Game Modes",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "• 2 Players: Play against a friend on the same device.\n" +
                "• Play with AI: Challenge our smart computer opponent.",
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShareScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Share App") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                Icons.Default.Share,
                contentDescription = null,
                modifier = Modifier.size(100.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                "Spread the fun!",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "Invite your friends to play Tic Tac Toe and see who's the ultimate champion!",
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(32.dp))
            Button(
                onClick = {
                    val sendIntent: Intent = Intent().apply {
                        action = Intent.ACTION_SEND
                        putExtra(Intent.EXTRA_TEXT, "Hey! Check out this awesome Tic Tac Toe app: https://play.google.com/store/apps/details?id=com.example.tictactoeapplication")
                        type = "text/plain"
                    }
                    val shareIntent = Intent.createChooser(sendIntent, null)
                    context.startActivity(shareIntent)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Share Now")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("About") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                "Tic Tac Toe Master",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text("Version 1.0.0", style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                "About the Game",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Tic Tac Toe is a classic two-player strategy game designed for fun and quick gameplay. Challenge your friends or test your skills against the computer in this simple yet engaging game.\n" +
                        "\n" +
                        "Features:\n" +
                        "\n" +
                        "Play against AI or another player\n" +
                        "Clean and user-friendly interface\n" +
                        "Smooth gameplay experience\n" +
                        "Lightweight and fast performance\n" +
                        "\n" +
                        "Developed to provide a fun and relaxing gaming experience for players of all ages.",
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyPolicyScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Privacy Policy") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                "Privacy Policy",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "1. Information Collection: We do not collect any personal information. All game data is stored locally on your device.\n\n" +
                "2. Permissions: This app does not require any special permissions to run.\n\n" +
                "3. Third-party Services: We do not share any data with third-party services.\n\n" +
                "4. Children's Privacy: Our game is safe for children and does not collect any data from them.\n\n" +
                "5. Changes to This Policy: We may update our Privacy Policy from time to time. We will notify you of any changes by posting the new Privacy Policy on this page.",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
