package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.data.local.ThemeMode
import com.example.ui.MainViewModel
import com.example.ui.screens.ExplanationScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.LessonDetailScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.ConverterTDTheme

object Routes {
    const val HOME = "home"
    const val EXPLANATION = "explanation"
    const val HISTORY = "history"
    const val SETTINGS = "settings"
    const val LESSON = "lesson/{topicId}"

    fun lesson(topicId: String) = "lesson/$topicId"
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: MainViewModel = viewModel()
            val settings by viewModel.settings.collectAsStateWithLifecycle()
            val systemDark = isSystemInDarkTheme()
            val isDark = when (settings.themeMode) {
                ThemeMode.SYSTEM -> systemDark
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }

            LaunchedEffect(isDark) {
                enableEdgeToEdge(
                    statusBarStyle = if (isDark) {
                        SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
                    } else {
                        SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
                    }
                )
            }

            ConverterTDTheme(darkTheme = isDark) {
                ConverterApp(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun ConverterApp(viewModel: MainViewModel) {
    val navController = rememberNavController()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val allHistory by viewModel.allHistory.collectAsStateWithLifecycle()
    val recentHistory by viewModel.recentHistory.collectAsStateWithLifecycle()
    val favoriteHistory by viewModel.favoriteHistory.collectAsStateWithLifecycle()

    var selectedTabIndex by rememberSaveable { mutableIntStateOf(0) }

    NavHost(
        navController = navController,
        startDestination = Routes.HOME,
        enterTransition = {
            slideInHorizontally(
                initialOffsetX = { it / 5 },
                animationSpec = tween(260)
            ) + fadeIn(animationSpec = tween(260))
        },
        exitTransition = {
            fadeOut(animationSpec = tween(180))
        },
        popEnterTransition = {
            fadeIn(animationSpec = tween(220))
        },
        popExitTransition = {
            slideOutHorizontally(
                targetOffsetX = { it / 5 },
                animationSpec = tween(240)
            ) + fadeOut(animationSpec = tween(220))
        }
    ) {
        composable(Routes.HOME) {
            HomeScreen(
                uiState = uiState,
                settings = settings,
                recentHistory = recentHistory,
                selectedTabIndex = selectedTabIndex,
                onTabSelected = { selectedTabIndex = it },
                onInputChange = viewModel::onInputChange,
                onClearInput = viewModel::clearInput,
                onSourceSystemSelected = viewModel::onSourceSystemSelected,
                onTargetSystemSelected = viewModel::onTargetSystemSelected,
                onSwapSystems = viewModel::swapSystems,
                onConvert = viewModel::convert,
                onUseResultAsInput = viewModel::useResultAsInput,
                onToggleFavorite = viewModel::toggleCurrentFavorite,
                onOpenExplanation = { navController.navigate(Routes.EXPLANATION) },
                onLoadHistoryItem = viewModel::loadFromHistory,
                onOpenHistory = { navController.navigate(Routes.HISTORY) },
                onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                onOpenLesson = { topicId -> navController.navigate(Routes.lesson(topicId)) }
            )
        }

        composable(Routes.EXPLANATION) {
            ExplanationScreen(
                result = uiState.currentResult,
                onBack = { navController.popBackStack() },
                onOpenSettings = { navController.navigate(Routes.SETTINGS) }
            )
        }

        composable(Routes.HISTORY) {
            HistoryScreen(
                allHistory = allHistory,
                favoriteHistory = favoriteHistory,
                onBack = { navController.popBackStack() },
                onSelectHistoryItem = { item ->
                    viewModel.loadFromHistory(item)
                    selectedTabIndex = 0
                    navController.popBackStack(Routes.HOME, inclusive = false)
                },
                onToggleFavorite = viewModel::toggleHistoryFavorite,
                onDeleteItem = viewModel::deleteHistoryItem,
                onClearAll = viewModel::clearHistory
            )
        }

        composable(Routes.SETTINGS) {
            SettingsScreen(
                settings = settings,
                onBack = { navController.popBackStack() },
                onThemeSelected = viewModel::updateThemeMode,
                onDecimalPrecisionChange = viewModel::updateDecimalPrecision,
                onMaxFractionDigitsChange = viewModel::updateMaxFractionDigits,
                onHapticToggle = viewModel::updateHapticFeedback,
                onClearHistory = viewModel::clearHistory,
                onResetSettings = viewModel::resetSettings
            )
        }

        composable(
            route = Routes.LESSON,
            arguments = listOf(navArgument("topicId") { type = NavType.StringType })
        ) { backStackEntry ->
            val topicId = backStackEntry.arguments?.getString("topicId").orEmpty()
            LessonDetailScreen(
                topicId = topicId,
                onBack = { navController.popBackStack() },
                onTryInConverter = { input, source, target ->
                    viewModel.loadConversionAndConvert(input, source, target)
                    selectedTabIndex = 0
                    navController.popBackStack(Routes.HOME, inclusive = false)
                }
            )
        }
    }
}
