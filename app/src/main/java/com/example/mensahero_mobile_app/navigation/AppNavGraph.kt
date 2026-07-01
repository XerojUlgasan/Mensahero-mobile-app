package com.example.mensahero_mobile_app.navigation

import android.content.Context
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.runtime.*
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.mensahero_mobile_app.ui.dashboard.DashboardScreenWrapper
import com.example.mensahero_mobile_app.ui.keys.KeyScreenWrapper
import com.example.mensahero_mobile_app.ui.login.LoginScreen
import com.example.mensahero_mobile_app.ui.servercheck.ServerConnectionCheckScreen
import com.example.mensahero_mobile_app.ui.settings.SettingsScreenWrapper
import com.example.mensahero_mobile_app.ui.splash.SplashScreen
import com.example.mensahero_mobile_app.viewmodel.*

private val fadeIn = fadeIn(animationSpec = tween(220))
private val fadeOut = fadeOut(animationSpec = tween(180))

@Composable
fun AppNavGraph(
    navController: NavHostController,
    context: Context
) {
    NavHost(
        navController = navController,
        startDestination = Routes.Splash.route,
        enterTransition = { fadeIn },
        exitTransition = { fadeOut },
        popEnterTransition = { fadeIn },
        popExitTransition = { fadeOut }
    ) {
        composable(Routes.Splash.route) {
            val viewModel = remember { SplashViewModel(context) }
            val state by viewModel.state.collectAsState()

            LaunchedEffect(state) {
                when (state) {
                    is SplashState.HasSession -> {
                        navController.navigate(Routes.ServerConnection.route) {
                            popUpTo(Routes.Splash.route) { inclusive = true }
                        }
                    }
                    is SplashState.NoSession -> {
                        navController.navigate(Routes.Login.route) {
                            popUpTo(Routes.Splash.route) { inclusive = true }
                        }
                    }
                    else -> {}
                }
            }

            SplashScreen(
                onTap = {
                    if (state is SplashState.Idle) {
                        viewModel.checkSession()
                    }
                }
            )
        }

        composable(Routes.Login.route) {
            val viewModel = remember { LoginViewModel(context) }
            LoginScreen(
                viewModel = viewModel,
                onLoginSuccess = {
                    navController.navigate(Routes.ServerConnection.route) {
                        popUpTo(Routes.Login.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.ServerConnection.route) {
            val viewModel = remember { ServerConnectionViewModel(context) }
            val state by viewModel.state.collectAsState()

            LaunchedEffect(state.isConnected) {
                if (state.isConnected) {
                    navController.navigate(Routes.Dashboard.route) {
                        popUpTo(Routes.ServerConnection.route) { inclusive = true }
                    }
                }
            }

            ServerConnectionCheckScreen(
                isLoading = state.isLoading,
                error = state.error,
                onRetry = { viewModel.checkConnection() }
            )
        }

        composable(Routes.Dashboard.route) {
            DashboardScreenWrapper(
                context = context,
                onNavigateToKeys = {
                    navController.navigate(Routes.Keys.route) {
                        popUpTo(Routes.Dashboard.route) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                onNavigateToSettings = {
                    navController.navigate(Routes.Settings.route) {
                        popUpTo(Routes.Dashboard.route) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }

        composable(Routes.Keys.route) {
            val viewModel = remember { KeysViewModel(context) }
            KeyScreenWrapper(
                viewModel = viewModel,
                onNavigateToDashboard = {
                    navController.navigate(Routes.Dashboard.route) {
                        popUpTo(Routes.Dashboard.route) { inclusive = true }
                        launchSingleTop = true
                    }
                },
                onNavigateToSettings = {
                    navController.navigate(Routes.Settings.route) {
                        popUpTo(Routes.Dashboard.route) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }

        composable(Routes.Settings.route) {
            val viewModel = remember { SettingsViewModel(context) }
            SettingsScreenWrapper(
                viewModel = viewModel,
                onNavigateToDashboard = {
                    navController.navigate(Routes.Dashboard.route) {
                        popUpTo(Routes.Dashboard.route) { inclusive = true }
                        launchSingleTop = true
                    }
                },
                onNavigateToKeys = {
                    navController.navigate(Routes.Keys.route) {
                        popUpTo(Routes.Dashboard.route) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                onLogoutSuccess = {
                    navController.navigate(Routes.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
    }
}
