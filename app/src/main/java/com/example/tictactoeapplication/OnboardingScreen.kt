package com.example.tictactoeapplication

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CellTower
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue

data class OnboardingPage(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val primaryColor: Color,
    val secondaryColor: Color
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(onFinished: () -> Unit) {
    val pages = listOf(
        OnboardingPage(
            title = "Classic Tic Tac Toe",
            description = "Enjoy the timeless game of strategy and skill, now beautifully designed for your device.",
            icon = Icons.Default.Gamepad,
            primaryColor = Color(0xFF3F51B5),
            secondaryColor = Color(0xFF7986CB)
        ),
        OnboardingPage(
            title = "Challenge the AI",
            description = "Test your wits against our smart AI. Perfect your moves and become a Tic Tac Toe master.",
            icon = Icons.Default.SmartToy,
            primaryColor = Color(0xFF00BCD4),
            secondaryColor = Color(0xFF4DD0E1)
        ),
        OnboardingPage(
            title = "Local Multiplayer",
            description = "Play with friends nearby using our seamless local multiplayer feature powered by Nearby Connections.",
            icon = Icons.Default.CellTower,
            primaryColor = Color(0xFF3F51B5),
            secondaryColor = Color(0xFF7986CB)
        ),
        OnboardingPage(
            title = "Ready to Play?",
            description = "Choose your mode, make your move, and have fun in the ultimate Tic Tac Toe experience!",
            icon = Icons.Default.Devices,
            primaryColor = Color(0xFF00BCD4),
            secondaryColor = Color(0xFF4DD0E1)
        )
    )

    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Background Decorative Elements
        OnboardingBackground()

        Column(modifier = Modifier.fillMaxSize()) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) { position ->
                OnboardingPageContent(
                    page = pages[position],
                    pagerState = pagerState,
                    pageIndex = position
                )
            }

            // Bottom controls
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp, vertical = 40.dp)
            ) {
                // Page Indicator (Centered)
                Row(
                    Modifier
                        .align(Alignment.CenterStart)
                        .height(32.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(pages.size) { iteration ->
                        val isSelected = pagerState.currentPage == iteration
                        val width by animateDpAsState(
                            targetValue = if (isSelected) 32.dp else 8.dp,
                            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                            label = ""
                        )
                        Box(
                            modifier = Modifier
                                .height(8.dp)
                                .width(width)
                                .clip(CircleShape)
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                )
                        )
                    }
                }

                // Buttons
                Row(
                    modifier = Modifier.align(Alignment.CenterEnd),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (pagerState.currentPage < pages.size - 1) {
                        TextButton(
                            onClick = onFinished,
                            modifier = Modifier.padding(end = 16.dp)
                        ) {
                            Text(
                                "Skip",
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                fontWeight = FontWeight.Medium
                            )
                        }
                        
                        Button(
                            onClick = {
                                scope.launch {
                                    pagerState.animateScrollToPage(pagerState.currentPage + 1)
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(12.dp),
                            modifier = Modifier.size(56.dp)
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = "Next"
                            )
                        }
                    } else {
                        Button(
                            onClick = onFinished,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .height(56.dp)
                                .fillMaxWidth(0.6f),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                        ) {
                            Text(
                                "Get Started",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun OnboardingBackground() {
    val primaryColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)
    val secondaryColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.05f)
    
    Canvas(modifier = Modifier.fillMaxSize()) {
        drawCircle(
            color = primaryColor,
            radius = size.width * 0.6f,
            center = Offset(size.width * 0.9f, size.height * 0.1f)
        )
        drawCircle(
            color = secondaryColor,
            radius = size.width * 0.4f,
            center = Offset(size.width * 0.1f, size.height * 0.9f)
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingPageContent(
    page: OnboardingPage,
    pagerState: PagerState,
    pageIndex: Int
) {
    val pageOffset = (pagerState.currentPage - pageIndex) + pagerState.currentPageOffsetFraction
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(40.dp)
            .graphicsLayer {
                // Parallax and fade effect
                alpha = 1f - pageOffset.absoluteValue.coerceIn(0f, 1f)
                translationX = pageOffset * size.width * 0.5f
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Animated Icon Container
        Box(
            modifier = Modifier
                .size(240.dp)
                .scale(1f - (pageOffset.absoluteValue * 0.2f))
                .clip(RoundedCornerShape(48.dp))
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            page.primaryColor.copy(alpha = 0.15f),
                            page.secondaryColor.copy(alpha = 0.05f)
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            // Decorative background circle for icon
            Surface(
                modifier = Modifier
                    .size(140.dp)
                    .alpha(0.5f),
                shape = CircleShape,
                color = page.primaryColor.copy(alpha = 0.1f)
            ) {}

            Icon(
                imageVector = page.icon,
                contentDescription = null,
                modifier = Modifier
                    .size(100.dp)
                    .graphicsLayer {
                        rotationZ = pageOffset * 45f
                    },
                tint = page.primaryColor
            )
        }
        
        Spacer(modifier = Modifier.height(60.dp))
        
        Text(
            text = page.title,
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.graphicsLayer {
                translationY = pageOffset.absoluteValue * 50f
            }
        )
        
        Spacer(modifier = Modifier.height(20.dp))
        
        Text(
            text = page.description,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 28.sp,
            modifier = Modifier
                .padding(horizontal = 8.dp)
                .graphicsLayer {
                    translationY = pageOffset.absoluteValue * 100f
                }
        )
    }
}

