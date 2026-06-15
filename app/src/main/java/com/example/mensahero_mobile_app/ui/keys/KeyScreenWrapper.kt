package com.example.mensahero_mobile_app.ui.keys

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mensahero_mobile_app.viewmodel.KeysViewModel
import com.mensahero.app.ui.screens.MensaBottomNav

private val BgBlack = Color(0xFF0A0A0A)
private val SurfaceDark = Color(0xFF1A1A1A)
private val BorderColor = Color(0xFF2A2A2A)
private val White = Color(0xFFFFFFFF)
private val TextMuted = Color(0xFF6B6B6B)
private val InputText = Color(0xFFE5E5E5)
private val GreenActive = Color(0xFF22C55E)

@Composable
fun KeyScreenWrapper(
    viewModel: KeysViewModel,
    onNavigateToDashboard: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    var keyVisible by remember { mutableStateOf(false) }
    var selectedNav by remember { mutableIntStateOf(1) }

    LaunchedEffect(state.savedSuccess) {
        if (state.savedSuccess) {
            kotlinx.coroutines.delay(1500)
        }
    }

    Scaffold(
        containerColor = BgBlack,
        bottomBar = {
            MensaBottomNav(
                selectedIndex = selectedNav,
                onItemSelected = { index ->
                    selectedNav = index
                    when (index) {
                        0 -> onNavigateToDashboard()
                        1 -> {} // Already on Keys
                        2 -> onNavigateToSettings()
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp)
                .statusBarsPadding()
        ) {
            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "API Key",
                color = White,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.5).sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Enter your MensaHERO API key to link this device to your account.",
                color = TextMuted,
                fontSize = 14.sp,
                lineHeight = 20.sp
            )

            Spacer(modifier = Modifier.height(36.dp))

            Text(
                text = "API Key",
                color = TextMuted,
                fontSize = 13.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            OutlinedTextField(
                value = state.apiKey,
                onValueChange = { viewModel.onApiKeyChange(it) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                visualTransformation = if (keyVisible) VisualTransformation.None
                else PasswordVisualTransformation('·'),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                shape = RoundedCornerShape(10.dp),
                trailingIcon = {
                    IconButton(onClick = { keyVisible = !keyVisible }) {
                        Icon(
                            imageVector = if (keyVisible) Icons.Outlined.VisibilityOff
                            else Icons.Outlined.Visibility,
                            contentDescription = if (keyVisible) "Hide key" else "Show key",
                            tint = TextMuted,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = SurfaceDark,
                    unfocusedContainerColor = SurfaceDark,
                    focusedBorderColor = Color(0xFF3A3A3A),
                    unfocusedBorderColor = BorderColor,
                    focusedTextColor = InputText,
                    unfocusedTextColor = InputText,
                    cursorColor = White
                ),
                textStyle = LocalTextStyle.current.copy(
                    fontSize = 14.sp,
                    fontFamily = FontFamily.Monospace
                )
            )

            Spacer(modifier = Modifier.height(28.dp))

            Button(
                onClick = { viewModel.saveApiKey(state.apiKey) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = White,
                    contentColor = Color(0xFF0A0A0A)
                )
            ) {
                Text(
                    text = "Save Key",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (state.savedSuccess) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "✓ Key saved successfully!",
                        color = GreenActive,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Outlined.Info,
                    contentDescription = null,
                    tint = TextMuted,
                    modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(5.dp))
                Text(
                    text = "Your key is stored securely on this device only.",
                    color = TextMuted,
                    fontSize = 12.sp
                )
            }
        }
    }
}
