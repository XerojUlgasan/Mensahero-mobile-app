package com.example.mensahero_mobile_app

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import com.example.mensahero_mobile_app.data.api.MensaheroApiService
import com.example.mensahero_mobile_app.data.datastore.PreferencesManager
import com.example.mensahero_mobile_app.data.fcm.FcmTokenManager
import com.example.mensahero_mobile_app.data.model.DeviceUpdateRequest
import com.example.mensahero_mobile_app.navigation.AppNavGraph
import com.example.mensahero_mobile_app.ui.theme.MensaheromobileappTheme
import com.google.firebase.FirebaseApp
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private lateinit var preferencesManager: PreferencesManager
    private val apiService = MensaheroApiService()

    private var showBatteryDialog by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        try {
            Log.d("Firebase", "Firebase initialized: ${FirebaseApp.getInstance().name}")
        } catch (e: Exception) {
            Log.e("Firebase", "Firebase initialization failed: ${e.message}", e)
        }

        preferencesManager = PreferencesManager(this)
        checkAndUpdateFcmToken()
        checkBatteryOptimization()

        setContent {
            MensaheromobileappTheme {
                val navController = rememberNavController()
                AppNavGraph(navController = navController, context = this)

                if (showBatteryDialog) {
                    BatteryOptimizationDialog(
                        isMiui = isMiuiDevice(),
                        onOpenSettings = { openBatteryOptimizationSettings() },
                        onOpenAutostart = { openMiuiAutostartSettings() },
                        onDismiss = { showBatteryDialog = false }
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Re-check each time user returns (e.g. after granting in system settings)
        checkBatteryOptimization()
    }

    private fun checkBatteryOptimization() {
        val powerManager = getSystemService(PowerManager::class.java)
        showBatteryDialog = !powerManager.isIgnoringBatteryOptimizations(packageName)
    }

    private fun openBatteryOptimizationSettings() {
        startActivity(
            Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = Uri.parse("package:$packageName")
            }
        )
    }

    private fun openMiuiAutostartSettings() {
        val autostartIntent = Intent().apply {
            component = android.content.ComponentName(
                "com.miui.securitycenter",
                "com.miui.permcenter.autostart.AutoStartManagementActivity"
            )
        }
        // Falls back to app info screen if MIUI autostart screen is unavailable
        runCatching { startActivity(autostartIntent) }.onFailure {
            startActivity(
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.parse("package:$packageName")
                }
            )
        }
    }

    private fun isMiuiDevice(): Boolean {
        return Build.MANUFACTURER.equals("xiaomi", ignoreCase = true) ||
                getSystemProperty("ro.miui.ui.version.name").isNotEmpty()
    }

    private fun getSystemProperty(key: String): String {
        return try {
            val clazz = Class.forName("android.os.SystemProperties")
            clazz.getMethod("get", String::class.java).invoke(null, key) as? String ?: ""
        } catch (e: Exception) {
            ""
        }
    }

    private fun checkAndUpdateFcmToken() {
        lifecycleScope.launch {
            try {
                val deviceId = preferencesManager.deviceId.first()
                val savedFcmToken = preferencesManager.fcmToken.first()
                val deviceName = preferencesManager.deviceName.first()
                val apiKey = preferencesManager.apiKey.first()

                Log.d("FCMToken", "Device ID: $deviceId")

                if (deviceId == null || savedFcmToken == null || deviceName == null || apiKey == null) {
                    Log.d("FCMToken", "Missing required data - skipping token check")
                    return@launch
                }

                val currentTokenResult = FcmTokenManager.getToken()
                if (currentTokenResult.isFailure) return@launch

                val currentToken = currentTokenResult.getOrNull() ?: return@launch

                Log.d("FCMToken", "Old FCM Token: $savedFcmToken")
                Log.d("FCMToken", "New FCM Token: $currentToken")

                if (currentToken == savedFcmToken) {
                    Log.d("FCMToken", "FCM tokens match - no update needed")
                    return@launch
                }

                Log.d("FCMToken", "FCM tokens differ. Updating to new token.")
                val result = apiService.updateDevice(
                    DeviceUpdateRequest(
                        device_id = deviceId,
                        api_key = apiKey,
                        fcm_token = currentToken,
                        isActive = true,
                        device_name = deviceName
                    )
                )

                if (result.isSuccess) {
                    Log.d("FCMToken", "Successfully updated FCM token on backend")
                    preferencesManager.saveFcmToken(currentToken)
                } else {
                    Log.e("FCMToken", "Failed to update FCM token: ${result.exceptionOrNull()?.message}")
                    Toast.makeText(
                        this@MainActivity,
                        "Unable to update FCM token. Background messaging may not work.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            } catch (e: Exception) {
                Toast.makeText(
                    this@MainActivity,
                    "Unable to update FCM token. Background messaging may not work.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}

@Composable
private fun BatteryOptimizationDialog(
    isMiui: Boolean,
    onOpenSettings: () -> Unit,
    onOpenAutostart: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Enable Background Processing") },
        text = {
            Text(
                if (isMiui)
                    "MensaHERO needs to run in the background to process messages reliably.\n\n" +
                    "Please do the following:\n" +
                    "1. Tap \"Disable Battery Optimization\" and select \"Allow\"\n" +
                    "2. Tap \"Enable Autostart\" and toggle it on for MensaHERO\n\n" +
                    "Without these, messages may not be sent when the app is closed."
                else
                    "MensaHERO needs to run in the background to process messages reliably.\n\n" +
                    "Tap \"Disable Battery Optimization\" and select \"Allow\" to ensure messages " +
                    "are sent even when the app is closed."
            )
        },
        confirmButton = {
            TextButton(onClick = onOpenSettings) {
                Text("Disable Battery Optimization")
            }
        },
        dismissButton = {
            if (isMiui) {
                TextButton(onClick = onOpenAutostart) {
                    Text("Enable Autostart")
                }
            } else {
                TextButton(onClick = onDismiss) {
                    Text("Later")
                }
            }
        }
    )
}
