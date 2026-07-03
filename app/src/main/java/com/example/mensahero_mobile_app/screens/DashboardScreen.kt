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
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.core.content.ContextCompat
import com.example.mensahero_mobile_app.data.model.SimInfo
import com.example.mensahero_mobile_app.viewmodel.DashboardViewModel
import com.example.mensahero_mobile_app.viewmodel.DashboardState

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
fun SimSelectionDialog(
    sims: List<SimInfo>,
    onSimSelected: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = SurfaceDark,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                Text(
                    text = "Select SIM Card",
                    color = White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "Choose the SIM to use for sending messages",
                    color = TextMuted,
                    fontSize = 14.sp
                )
                
                Spacer(modifier = Modifier.height(20.dp))
                
                sims.forEach { sim ->
                    Surface(
                        onClick = { onSimSelected(sim.subscriptionId) },
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF2A2A2A),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.SimCard,
                                contentDescription = null,
                                tint = White,
                                modifier = Modifier.size(24.dp)
                            )
                            
                            Spacer(modifier = Modifier.width(12.dp))
                            
                            Column {
                                Text(
                                    text = sim.carrierName,
                                    color = White,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = if (sim.phoneNumber.isNullOrBlank()) "Number hidden" 
                                           else sim.phoneNumber,
                                    color = TextMuted,
                                    fontSize = 13.sp
                                )
                            }
                            
                            Spacer(modifier = Modifier.weight(1f))
                            
                            Text(
                                text = "SIM ${sim.slotIndex + 1}",
                                color = TextMuted,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DeviceRegistrationDialog(
    show: Boolean,
    deviceName: String,
    apiKey: String = "",
    isRegistering: Boolean,
    error: String?,
    isUpdate: Boolean = false,
    onDeviceNameChange: (String) -> Unit,
    onApiKeyChange: (String) -> Unit = {},
    onRegister: () -> Unit,
    onDismiss: () -> Unit
) {
    if (show) {
        Dialog(onDismissRequest = onDismiss) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = SurfaceDark,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text(
                        text = if (isUpdate) "Edit Device Name" else "Register Device",
                        color = White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (isUpdate) "Update your device name" else "Enter your device details to register",
                        color = TextMuted,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(20.dp))

                    if (!isUpdate) {
                        OutlinedTextField(
                            value = apiKey,
                            onValueChange = onApiKeyChange,
                            label = { Text("API Key", color = TextMuted) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            shape = RoundedCornerShape(10.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color(0xFF2A2A2A),
                                unfocusedContainerColor = Color(0xFF2A2A2A),
                                focusedBorderColor = Color(0xFF3A3A3A),
                                unfocusedBorderColor = BorderColor,
                                focusedTextColor = White,
                                unfocusedTextColor = White,
                                cursorColor = White
                            )
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    OutlinedTextField(
                        value = deviceName,
                        onValueChange = onDeviceNameChange,
                        label = { Text("Device Name", color = TextMuted) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFF2A2A2A),
                            unfocusedContainerColor = Color(0xFF2A2A2A),
                            focusedBorderColor = Color(0xFF3A3A3A),
                            unfocusedBorderColor = BorderColor,
                            focusedTextColor = White,
                            unfocusedTextColor = White,
                            cursorColor = White
                        )
                    )

                    if (error != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = error!!,
                            color = Color(0xFFDC2626),
                            fontSize = 12.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = TextMuted
                            )
                        ) {
                            Text("Cancel", fontSize = 14.sp)
                        }
                        Button(
                            onClick = onRegister,
                            enabled = !isRegistering,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = White,
                                contentColor = BgBlack
                            )
                        ) {
                            if (isRegistering) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = BgBlack,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text(if (isUpdate) "Update" else "Register", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
        }
    }
}

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
fun AgentStatusCard(agent: AgentInfo, onEditDeviceName: () -> Unit = {}) {
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

            // Device name with edit button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = agent.name,
                    color = White,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = onEditDeviceName,
                    modifier = Modifier.size(32.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = White,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Edit,
                            contentDescription = "Edit device name",
                            tint = BgBlack,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

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
    viewModel: DashboardViewModel,
    onNavSelected: (Int) -> Unit = {}
) {
    val context = LocalContext.current
    var selectedNav by remember { mutableIntStateOf(0) }
    val state by viewModel.state.collectAsState()
    
    val swipeRefreshState = rememberSwipeRefreshState(
        isRefreshing = state.isFetching
    )

    val simLabel = if (state.chosenSimId != null) "Selected" else "Not set"

    val stats = listOf(
        StatItem("Messages Today", state.totalMessagesFetched.toString(), Icons.Outlined.ChatBubbleOutline),
        StatItem("Delivery Rate", "${String.format("%.1f", viewModel.getDeliveryRate())}%", Icons.Outlined.TrendingUp),
        StatItem("Chosen SIM", simLabel, Icons.Outlined.SimCard),
        StatItem("Last Activity", viewModel.getLastActivityText(), Icons.Outlined.AccessTime)
    )

    val agent = AgentInfo(
        name = if (state.deviceName.isNotEmpty()) state.deviceName else "${Build.MANUFACTURER.replaceFirstChar { it.uppercase() }} ${Build.MODEL}",
        isOnline = state.agentActive,
        uptime = viewModel.getUptimeText(),
        gateway = viewModel.getApiUrl()
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
        SwipeRefresh(
            state = swipeRefreshState,
            onRefresh = {
                if (state.canRefresh && !state.isFetching && !state.isProcessing) {
                    viewModel.manualRefresh()
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
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
                            checked = state.agentActive,
                            onCheckedChange = { viewModel.toggleAgentActive(it) },
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
                            text = if (state.agentActive) "Active" else "Inactive",
                            color = if (state.agentActive) GreenActive else TextMuted,
                            fontSize = 11.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                // Processing indicator
                if (state.isProcessing) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF1A3A2A),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = GreenActive,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Processing ${state.processingMessageCount} messages...",
                                color = GreenActive,
                                fontSize = 13.sp
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Device registration prompt
                if (!state.isCheckingRegistration && !state.isDeviceRegistered) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF3A2A1A),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "Device Not Registered",
                                color = Color(0xFFFFB84D),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Register your device to enable message fetching",
                                color = TextMuted,
                                fontSize = 12.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = { viewModel.showDeviceRegistration() },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFFFB84D),
                                    contentColor = Color(0xFF1A1A1A)
                                )
                            ) {
                                Text("Register Device", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatCard(stats[0], modifier = Modifier.weight(1f))
                    StatCard(stats[1], modifier = Modifier.weight(1f))
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
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

                AgentStatusCard(
                    agent = agent,
                    onEditDeviceName = {
                        viewModel.showDeviceRegistration()
                    }
                )

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
    
    if (state.showSimSelection) {
        SimSelectionDialog(
            sims = state.availableSims,
            onSimSelected = { viewModel.selectSim(it) },
            onDismiss = { }
        )
    }

    DeviceRegistrationDialog(
        show = state.showDeviceRegistration,
        deviceName = state.inputDeviceName,
        apiKey = state.inputApiKey,
        isRegistering = state.isRegisteringDevice,
        error = state.deviceRegistrationError,
        isUpdate = state.isDeviceRegistered,
        onDeviceNameChange = { viewModel.onInputDeviceNameChange(it) },
        onApiKeyChange = { viewModel.onInputApiKeyChange(it) },
        onRegister = { viewModel.registerDevice() },
        onDismiss = { viewModel.hideDeviceRegistration() }
    )
}

