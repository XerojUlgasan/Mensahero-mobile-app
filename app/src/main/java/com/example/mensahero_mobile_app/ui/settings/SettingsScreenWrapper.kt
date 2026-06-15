package com.example.mensahero_mobile_app.ui.settings

import androidx.compose.runtime.*
import com.example.mensahero_mobile_app.viewmodel.SettingsViewModel
import com.mensahero.app.ui.screens.SettingScreen
import com.mensahero.app.ui.screens.UserProfile
import com.mensahero.app.ui.screens.AppBuildInfo

@Composable
fun SettingsScreenWrapper(
    viewModel: SettingsViewModel,
    onNavigateToDashboard: () -> Unit,
    onNavigateToKeys: () -> Unit,
    onLogoutSuccess: () -> Unit
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(state.logoutSuccess) {
        if (state.logoutSuccess) {
            onLogoutSuccess()
        }
    }

    SettingScreen(
        user = UserProfile(
            name = state.userName.ifEmpty { "User" },
            email = state.userEmail,
            initials = state.userName.take(2).uppercase().ifEmpty { "U" }
        ),
        buildInfo = AppBuildInfo(
            version = "v0.1.0-alpha",
            build = "2024.06.001"
        ),
        onLogOut = {
            viewModel.logout()
        },
        onNavSelected = { index ->
            when (index) {
                0 -> onNavigateToDashboard()
                1 -> onNavigateToKeys()
                2 -> {} // Already on Settings
            }
        }
    )
}
