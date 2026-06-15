package com.example.mensahero_mobile_app.ui.dashboard

import android.Manifest
import androidx.compose.runtime.*
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.isGranted
import com.mensahero.app.ui.screens.DashboardScreen

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun DashboardScreenWrapper(
    onNavigateToKeys: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val phonePermission = rememberPermissionState(Manifest.permission.READ_PHONE_STATE)

    LaunchedEffect(Unit) {
        if (!phonePermission.status.isGranted) {
            phonePermission.launchPermissionRequest()
        }
    }

    DashboardScreen(
        onNavSelected = { index ->
            when (index) {
                0 -> {}
                1 -> onNavigateToKeys()
                2 -> onNavigateToSettings()
            }
        }
    )
}
