package com.mensahero.app.ui.screens

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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Colors
private val BgBlack = Color(0xFF0A0A0A)
private val SurfaceDark = Color(0xFF1A1A1A)
private val BorderColor = Color(0xFF2A2A2A)
private val White = Color(0xFFFFFFFF)
private val TextMuted = Color(0xFF6B6B6B)
private val TextPrimary = Color(0xFFF5F5F5)
private val InputText = Color(0xFFE5E5E5)

@Composable
fun KeyScreen(
    onSaveKey: (key: String) -> Unit = {},
    onNavSelected: (Int) -> Unit = {}
) {
    var apiKey by remember { mutableStateOf("mk_live_············3f9a") }
    var keyVisible by remember { mutableStateOf(false) }
    var selectedNav by remember { mutableIntStateOf(1) }

    Scaffold(
        containerColor = BgBlack,
        bottomBar = {
            MensaBottomNav(
                selectedIndex = selectedNav,
                onItemSelected = {
                    selectedNav = it
                    onNavSelected(it)
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

            // Page title
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

            // API Key label
            Text(
                text = "API Key",
                color = TextMuted,
                fontSize = 13.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // API Key input
            OutlinedTextField(
                value = apiKey,
                onValueChange = { apiKey = it },
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

            // Save Key button
            Button(
                onClick = { onSaveKey(apiKey) },
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

            // Security note
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

@Preview(
    showBackground = true,
    backgroundColor = 0xFF0A0A0A,
    widthDp = 375,
    heightDp = 812
)
@Composable
fun KeyScreenPreview() {
    KeyScreen()
}
