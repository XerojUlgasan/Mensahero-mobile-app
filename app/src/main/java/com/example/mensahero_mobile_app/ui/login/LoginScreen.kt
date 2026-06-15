package com.example.mensahero_mobile_app.ui.login

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mensahero_mobile_app.viewmodel.LoginViewModel

private val BgBlack = Color(0xFF0A0A0A)
private val SurfaceDark = Color(0xFF1A1A1A)
private val BorderColor = Color(0xFF2A2A2A)
private val White = Color(0xFFFFFFFF)
private val TextMuted = Color(0xFF6B6B6B)
private val TextPrimary = Color(0xFFF5F5F5)
private val InputText = Color(0xFFE5E5E5)
private val ErrorRed = Color(0xFFDC2626)

@Composable
fun LoginScreen(
    viewModel: LoginViewModel,
    onLoginSuccess: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    var passwordVisible by remember { mutableStateOf(false) }

    LaunchedEffect(state.loginSuccess) {
        if (state.loginSuccess) {
            onLoginSuccess()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgBlack)
            .padding(horizontal = 24.dp)
            .statusBarsPadding()
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "MensaHERO",
            color = TextPrimary,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )

        Spacer(modifier = Modifier.height(48.dp))

        Text(
            text = "Welcome back",
            color = White,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.5).sp
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "Sign in to manage your SMS agent.",
            color = TextMuted,
            fontSize = 15.sp
        )

        Spacer(modifier = Modifier.height(40.dp))

        // Email field
        Text(
            text = "Email",
            color = TextMuted,
            fontSize = 13.sp,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        OutlinedTextField(
            value = state.email,
            onValueChange = { viewModel.onEmailChange(it) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            shape = RoundedCornerShape(10.dp),
            isError = state.emailError != null,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = SurfaceDark,
                unfocusedContainerColor = SurfaceDark,
                errorContainerColor = SurfaceDark,
                focusedBorderColor = Color(0xFF3A3A3A),
                unfocusedBorderColor = BorderColor,
                errorBorderColor = ErrorRed,
                focusedTextColor = InputText,
                unfocusedTextColor = InputText,
                cursorColor = White
            ),
            textStyle = LocalTextStyle.current.copy(
                fontSize = 15.sp,
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
            )
        )

        if (state.emailError != null) {
            Text(
                text = state.emailError!!,
                color = ErrorRed,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Password field
        Text(
            text = "Password",
            color = TextMuted,
            fontSize = 13.sp,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        OutlinedTextField(
            value = state.password,
            onValueChange = { viewModel.onPasswordChange(it) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            visualTransformation = if (passwordVisible) VisualTransformation.None
            else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            shape = RoundedCornerShape(10.dp),
            isError = state.passwordError != null,
            trailingIcon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        imageVector = if (passwordVisible) Icons.Outlined.VisibilityOff
                        else Icons.Outlined.Visibility,
                        contentDescription = if (passwordVisible) "Hide password"
                        else "Show password",
                        tint = TextMuted,
                        modifier = Modifier.size(20.dp)
                    )
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = SurfaceDark,
                unfocusedContainerColor = SurfaceDark,
                errorContainerColor = SurfaceDark,
                focusedBorderColor = Color(0xFF3A3A3A),
                unfocusedBorderColor = BorderColor,
                errorBorderColor = ErrorRed,
                focusedTextColor = InputText,
                unfocusedTextColor = InputText,
                cursorColor = White
            ),
            textStyle = LocalTextStyle.current.copy(fontSize = 15.sp)
        )

        if (state.passwordError != null) {
            Text(
                text = state.passwordError!!,
                color = ErrorRed,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Error message from API
        if (state.errorMessage != null) {
            Text(
                text = state.errorMessage!!,
                color = ErrorRed,
                fontSize = 13.sp,
                modifier = Modifier.padding(bottom = 12.dp)
            )
        }

        // Sign In button
        Button(
            onClick = { viewModel.login() },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(10.dp),
            enabled = !state.isLoading,
            colors = ButtonDefaults.buttonColors(
                containerColor = White,
                contentColor = Color(0xFF0A0A0A)
            )
        ) {
            if (state.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = Color(0xFF0A0A0A)
                )
            } else {
                Text(
                    text = "Sign In",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
    }
}
