package com.mensahero.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
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
private val GreenActive = Color(0xFF22C55E)
private val DangerRed = Color(0xFFDC2626)
private val DangerRedDim = Color(0xFF1F0A0A)

data class UserProfile(
    val name: String,
    val email: String,
    val initials: String
)

data class AppBuildInfo(
    val version: String,
    val build: String
)

@Composable
fun AvatarInitials(
    initials: String,
    size: Int = 44,
    fontSize: Int = 15
) {
    Box(
        modifier = Modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(Color(0xFF2A2A2A)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initials,
            color = White,
            fontSize = fontSize.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun SectionLabel(text: String) {
    Text(
        text = text,
        color = TextMuted,
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 1.2.sp,
        modifier = Modifier.padding(bottom = 10.dp)
    )
}

@Composable
fun InfoRow(
    label: String,
    value: String,
    isLast: Boolean = false
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                color = TextPrimary,
                fontSize = 14.sp
            )
            Text(
                text = value,
                color = TextMuted,
                fontSize = 14.sp,
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
            )
        }
        if (!isLast) {
            HorizontalDivider(
                color = BorderColor,
                thickness = 0.5.dp
            )
        }
    }
}

@Composable
fun SettingScreen(
    user: UserProfile = UserProfile(
        name = "Alex Reyes",
        email = "agent@mensahero.dev",
        initials = "AR"
    ),
    buildInfo: AppBuildInfo = AppBuildInfo(
        version = "v0.1.0-alpha",
        build = "2024.06.001"
    ),
    onLogOut: () -> Unit = {},
    onNavSelected: (Int) -> Unit = {}
) {
    var selectedNav by remember { mutableIntStateOf(2) }
    var showLogoutConfirm by remember { mutableStateOf(false) }

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
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .statusBarsPadding()
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Top bar wordmark + active toggle (mirrors Dashboard)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "MensaHERO",
                    color = White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.3).sp
                )
                Column(horizontalAlignment = Alignment.End) {
                    Switch(
                        checked = true,
                        onCheckedChange = {},
                        enabled = false,
                        colors = SwitchDefaults.colors(
                            disabledCheckedThumbColor = White,
                            disabledCheckedTrackColor = Color(0xFF16A34A),
                            disabledCheckedBorderColor = Color.Transparent
                        ),
                        modifier = Modifier.height(28.dp)
                    )
                    Text(
                        text = "Active",
                        color = GreenActive,
                        fontSize = 11.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Page title
            Text(
                text = "Settings",
                color = White,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.5).sp
            )

            Spacer(modifier = Modifier.height(28.dp))

            // ── ACCOUNT section ──
            SectionLabel("ACCOUNT")

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = SurfaceDark,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AvatarInitials(initials = user.initials)
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text(
                            text = user.name,
                            color = White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = user.email,
                            color = TextMuted,
                            fontSize = 13.sp,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // ── APP section ──
            SectionLabel("APP")

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = SurfaceDark,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    InfoRow(label = "App Version", value = buildInfo.version)
                    InfoRow(label = "Build", value = buildInfo.build, isLast = true)
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // ── DANGER ZONE section ──
            Text(
                text = "DANGER ZONE",
                color = DangerRed,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.2.sp,
                modifier = Modifier.padding(bottom = 10.dp)
            )

            if (!showLogoutConfirm) {
                OutlinedButton(
                    onClick = { showLogoutConfirm = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, DangerRed),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = DangerRedDim,
                        contentColor = DangerRed
                    )
                ) {
                    Text(
                        text = "Log Out",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = DangerRed
                    )
                }
            } else {
                // Inline confirmation
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = DangerRedDim,
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, DangerRed, RoundedCornerShape(10.dp))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Are you sure you want to log out?",
                            color = TextPrimary,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedButton(
                                onClick = { showLogoutConfirm = false },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp),
                                border = androidx.compose.foundation.BorderStroke(
                                    0.5.dp, BorderColor
                                ),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = TextMuted
                                )
                            ) {
                                Text("Cancel", fontSize = 14.sp)
                            }
                            Button(
                                onClick = {
                                    showLogoutConfirm = false
                                    onLogOut()
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = DangerRed,
                                    contentColor = White
                                )
                            ) {
                                Text("Log Out", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
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
fun SettingScreenPreview() {
    SettingScreen()
}
