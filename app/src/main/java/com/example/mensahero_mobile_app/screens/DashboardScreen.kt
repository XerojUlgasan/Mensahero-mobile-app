package com.mensahero.app.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.telephony.SubscriptionManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat

// Colors
private val BgBlack = Color(0xFF0A0A0A)
private val SurfaceDark = Color(0xFF1A1A1A)
private val BorderColor = Color(0xFF2A2A2A)
private val White = Color(0xFFFFFFFF)
private val TextMuted = Color(0xFF6B6B6B)
private val TextPrimary = Color(0xFFF5F5F5)
private val GreenActive = Color(0xFF22C55E)
private val GreenDim = Color(0xFF16A34A)
private val NavSelected = Color(0xFFFFFFFF)
private val NavUnselected = Color(0xFF6B6B6B)

// ── Data models ──────────────────────────────────────────────

data class StatItem(
    val label: String,
    val value: String,
    val icon: ImageVector
)

data class AgentInfo(
    val name: String,
    val isOnline: Boolean,
    val uptime: String,
    val gateway: String
)

// ── Sub-components ───────────────────────────────────────────

@Composable
fun StatCard(stat: StatItem, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = SurfaceDark,
        tonalElevation = 0.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stat.label,
                    color = TextMuted,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Normal
                )
                Icon(
                    imageVector = stat.icon,
                    contentDescription = null,
                    tint = TextMuted,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = stat.value,
                color = White,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.5).sp
            )
        }
    }
}

@Composable
fun AgentStatusCard(agent: AgentInfo) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = SurfaceDark
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Online badge
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = Color(0xFF0D2B1A)
            ) {
                Text(
                    text = "ONLINE",
                    color = GreenActive,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Device name
            Text(
                text = agent.name,
                color = White,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Uptime
            Text(
                text = "Uptime: ${agent.uptime}",
                color = TextMuted,
                fontSize = 13.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Gateway URL row
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.Router,
                    contentDescription = null,
                    tint = TextMuted,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = agent.gateway,
                    color = TextMuted,
                    fontSize = 13.sp,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                )
            }
        }
    }
}

@Composable
fun MensaBottomNav(
    selectedIndex: Int,
    onItemSelected: (Int) -> Unit
) {
    val items = listOf(
        Pair("Home", Icons.Outlined.GridView),
        Pair("API Key", Icons.Outlined.VpnKey),
        Pair("Settings", Icons.Outlined.PersonOutline)
    )

    Surface(
        color = SurfaceDark,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            items.forEachIndexed { index, (label, icon) ->
                val selected = index == selectedIndex
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .weight(1f)
                        .padding(vertical = 4.dp)
                ) {
                    IconButton(onClick = { onItemSelected(index) }) {
                        Icon(
                            imageVector = icon,
                            contentDescription = label,
                            tint = if (selected) NavSelected else NavUnselected,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Text(
                        text = label,
                        color = if (selected) NavSelected else NavUnselected,
                        fontSize = 11.sp,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                    )
                }
            }
        }
    }
}

// ── Main screen ──────────────────────────────────────────────

@Composable
fun DashboardScreen(
    onNavSelected: (Int) -> Unit = {}
) {
    val context = LocalContext.current
    var selectedNav by remember { mutableIntStateOf(0) }
    var agentActive by remember { mutableStateOf(true) }

    val activeSimCount = remember {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE)
            == PackageManager.PERMISSION_GRANTED
        ) {
            val sm = context.getSystemService(SubscriptionManager::class.java)
            sm?.activeSubscriptionInfoList?.size ?: 0
        } else 0
    }

    val simLabel = if (activeSimCount == 0) "—" else "$activeSimCount / $activeSimCount"

    val stats = listOf(
        StatItem("Messages Today", "1,284", Icons.Outlined.ChatBubbleOutline),
        StatItem("Delivery Rate", "98.3%", Icons.Outlined.TrendingUp),
        StatItem("Active SIMs", simLabel, Icons.Outlined.SimCard),
        StatItem("Last Activity", "2 min ago", Icons.Outlined.AccessTime)
    )

    val agent = AgentInfo(
        name = "${Build.MANUFACTURER.replaceFirstChar { it.uppercase() }} ${Build.MODEL}",
        isOnline = true,
        uptime = "14h 32m",
        gateway = "gateway.mensahero.dev"
    )

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

            // Top bar
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

                // Active toggle
                Column(horizontalAlignment = Alignment.End) {
                    Switch(
                        checked = agentActive,
                        onCheckedChange = { agentActive = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = White,
                            checkedTrackColor = GreenDim,
                            uncheckedThumbColor = TextMuted,
                            uncheckedTrackColor = SurfaceDark,
                            uncheckedBorderColor = BorderColor
                        ),
                        modifier = Modifier.height(28.dp)
                    )
                    Text(
                        text = if (agentActive) "Active" else "Inactive",
                        color = if (agentActive) GreenActive else TextMuted,
                        fontSize = 11.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Page title
            Text(
                text = "Dashboard",
                color = White,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.5).sp
            )

            Spacer(modifier = Modifier.height(20.dp))

            // 2×2 stat grid
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard(stats[0], modifier = Modifier.weight(1f))
                StatCard(stats[1], modifier = Modifier.weight(1f))
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard(stats[2], modifier = Modifier.weight(1f))
                StatCard(stats[3], modifier = Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Agent status header
            Text(
                text = "AGENT STATUS",
                color = TextMuted,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.2.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            AgentStatusCard(agent)

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
fun DashboardScreenPreview() {
    DashboardScreen()
}
