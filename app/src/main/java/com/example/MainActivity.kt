package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.model.AppScreen
import com.example.model.ThemeMode
import com.example.ui.components.GlyphFloatingNavBar
import com.example.ui.screens.GlyphGeminiScreen
import com.example.ui.screens.GlyphHomeScreen
import com.example.ui.screens.GlyphPatternsScreen
import com.example.ui.screens.GlyphSettingsScreen
import com.example.ui.screens.GlyphTimerScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.GlyphViewModel

class MainActivity : ComponentActivity() {
    private val glyphViewModel: GlyphViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val themeMode by glyphViewModel.themeMode.collectAsState()
            val systemInDark = isSystemInDarkTheme()
            val isDark = when (themeMode) {
                ThemeMode.DARK -> true
                ThemeMode.LIGHT -> false
                ThemeMode.SYSTEM -> systemInDark
            }

            MyApplicationTheme(darkTheme = isDark) {
                MainAppContainer(viewModel = glyphViewModel)
            }
        }
    }
}

@Composable
fun MainAppContainer(viewModel: GlyphViewModel) {
    val currentScreen by viewModel.currentScreen.collectAsState()

    Box(modifier = Modifier.fillMaxSize()) {
        // Main Screen View with iOS-like smooth spring slide & fade transition
        AnimatedContent(
            targetState = currentScreen,
            transitionSpec = {
                (slideInHorizontally(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    ),
                    initialOffsetX = { fullWidth -> if (targetState.ordinal > initialState.ordinal) fullWidth / 2 else -fullWidth / 2 }
                ) + fadeIn(
                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
                )).togetherWith(
                    slideOutHorizontally(
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioNoBouncy,
                            stiffness = Spring.StiffnessMediumLow
                        ),
                        targetOffsetX = { fullWidth -> if (targetState.ordinal > initialState.ordinal) -fullWidth / 2 else fullWidth / 2 }
                    ) + fadeOut(
                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
                    )
                )
            },
            label = "screen_transition"
        ) { screen ->
            when (screen) {
                AppScreen.VISUALS -> GlyphHomeScreen(viewModel = viewModel)
                AppScreen.TIMER -> GlyphTimerScreen(
                    viewModel = viewModel,
                    onBackToHome = { viewModel.navigateTo(AppScreen.VISUALS) }
                )
                AppScreen.PATTERNS -> GlyphPatternsScreen(
                    viewModel = viewModel,
                    onBackToHome = { viewModel.navigateTo(AppScreen.VISUALS) },
                    onGoToGemini = { viewModel.navigateTo(AppScreen.GEMINI) }
                )
                AppScreen.GEMINI -> GlyphGeminiScreen(
                    viewModel = viewModel,
                    onBackToHome = { viewModel.navigateTo(AppScreen.VISUALS) }
                )
                AppScreen.SETTINGS -> GlyphSettingsScreen(
                    viewModel = viewModel,
                    onBackToHome = { viewModel.navigateTo(AppScreen.VISUALS) }
                )
            }
        }

        // Floating Bottom Navigation Bar (Home, Timer, Call, Gemini, Settings)
        GlyphFloatingNavBar(
            currentScreen = currentScreen,
            onScreenSelected = { targetScreen ->
                viewModel.navigateTo(targetScreen)
            },
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

