package com.example.mensahero_mobile_app

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import com.example.mensahero_mobile_app.data.api.MensaheroApiService
import com.example.mensahero_mobile_app.data.datastore.PreferencesManager
import com.example.mensahero_mobile_app.data.fcm.FcmTokenManager
import com.example.mensahero_mobile_app.data.model.DeviceUpdateRequest
import com.example.mensahero_mobile_app.navigation.AppNavGraph
import com.example.mensahero_mobile_app.ui.theme.MensaheromobileappTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private lateinit var preferencesManager: PreferencesManager
    private val apiService = MensaheroApiService()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        preferencesManager = PreferencesManager(this)
        checkAndUpdateFcmToken()

        setContent {
            MensaheromobileappTheme {
                val navController = rememberNavController()
                AppNavGraph(
                    navController = navController,
                    context = this
                )
            }
        }
    }

    private fun checkAndUpdateFcmToken() {
        lifecycleScope.launch {
            try {
                val deviceId = preferencesManager.deviceId.first()
                val savedFcmToken = preferencesManager.fcmToken.first()
                val deviceName = preferencesManager.deviceName.first()
                val apiKey = preferencesManager.apiKey.first()

                if (deviceId == null || savedFcmToken == null || deviceName == null || apiKey == null) {
                    return@launch
                }

                val currentTokenResult = FcmTokenManager.getToken()
                if (currentTokenResult.isFailure) {
                    return@launch
                }

                val currentToken = currentTokenResult.getOrNull() ?: return@launch

                if (currentToken != savedFcmToken) {
                    val request = DeviceUpdateRequest(
                        device_id = deviceId,
                        api_key = apiKey,
                        fcm_token = currentToken,
                        isActive = true,
                        device_name = deviceName
                    )

                    val result = apiService.updateDevice(request)
                    if (result.isSuccess) {
                        preferencesManager.saveFcmToken(currentToken)
                    } else {
                        Toast.makeText(
                            this@MainActivity,
                            "Unable to update FCM token. Background messaging may not work.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
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