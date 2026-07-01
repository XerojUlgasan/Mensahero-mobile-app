package com.example.mensahero_mobile_app.ui.servercheck

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val BgBlack = Color(0xFF0A0A0A)
private val White = Color(0xFFFFFFFF)
private val TextMuted = Color(0xFF6B6B6B)
private val ErrorRed = Color(0xFFDC2626)

@Composable
fun ServerConnectionCheckScreen(
    isLoading: Boolean = true,
    error: String? = null,
    onRetry: () -> Unit = {}
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BgBlack),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(48.dp),
                    color = White
                )
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "Checking server connection...",
                    color = White,
                    fontSize = 16.sp
                )
            } else if (error != null) {
                Text(
                    text = "Unable to connect to server",
                    color = ErrorRed,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Please try again later",
                    color = TextMuted,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = onRetry,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = White,
                        contentColor = BgBlack
                    )
                ) {
                    Text(
                        text = "Retry",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}
