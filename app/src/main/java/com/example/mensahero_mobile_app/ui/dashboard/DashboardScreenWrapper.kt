package com.example.mensahero_mobile_app.ui.dashboard

import android.Manifest
import android.content.Context
import android.util.Log
import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.accompanist.permissions.isGranted
import com.mensahero.app.ui.screens.DashboardScreen
import com.example.mensahero_mobile_app.viewmodel.DashboardViewModel
import com.example.mensahero_mobile_app.viewmodel.DashboardViewModelFactory

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun DashboardScreenWrapper(
    context: Context,
    onNavigateToKeys: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val permissions = rememberMultiplePermissionsState(
        permissions = listOf(
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.SEND_SMS,
            Manifest.permission.POST_NOTIFICATIONS
        )
    )
    val viewModel: DashboardViewModel = viewModel(
        factory = DashboardViewModelFactory(context)
    )

    LaunchedEffect(Unit) {
        Log.d("DashboardScreenWrapper", "Checking permissions...")
        Log.d("DashboardScreenWrapper", "READ_PHONE_STATE granted: ${permissions.permissions[0].status.isGranted}")
        Log.d("DashboardScreenWrapper", "SEND_SMS granted: ${permissions.permissions[1].status.isGranted}")
        Log.d("DashboardScreenWrapper", "POST_NOTIFICATIONS granted: ${permissions.permissions[2].status.isGranted}")

        if (!permissions.allPermissionsGranted) {
            Log.d("DashboardScreenWrapper", "Requesting permissions...")
            permissions.launchMultiplePermissionRequest()
        } else {
            Log.d("DashboardScreenWrapper", "All permissions already granted")
        }
    }

    LaunchedEffect(permissions.allPermissionsGranted) {
        Log.d("DashboardScreenWrapper", "Permission status changed: ${permissions.allPermissionsGranted}")
    }

    DashboardScreen(
        viewModel = viewModel,
        onNavSelected = { index ->
            when (index) {
                0 -> {}
                1 -> onNavigateToKeys()
                2 -> onNavigateToSettings()
            }
        }
    )
}
