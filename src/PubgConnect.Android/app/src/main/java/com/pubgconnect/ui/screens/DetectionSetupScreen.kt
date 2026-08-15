package com.pubgconnect.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pubgconnect.detection.PubgDetector
import com.pubgconnect.ui.components.GlassCard
import com.pubgconnect.ui.components.PrimaryButton
import com.pubgconnect.ui.components.PubgDivider
import com.pubgconnect.ui.components.SecondaryButton
import com.pubgconnect.ui.theme.*
import com.pubgconnect.ui.viewmodels.MainViewModel

@Composable
fun DetectionSetupScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val hasUsageAccess by viewModel.hasUsageAccess.collectAsState()
    val isPubgInstalled by viewModel.isPubgInstalled.collectAsState()
    val isSimulatedActive by viewModel.isSimulatedPubgActive.collectAsState()
    var isSimModeEnabled by remember { mutableStateOf(viewModel.sessionManager.isSimulationMode) }

    LaunchedEffect(Unit) {
        viewModel.refreshUsageAccessStatus()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDark)
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "PUBG DETECTION SETUP",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
        Text(
            text = "Configure Android Usage Access and status detection",
            fontSize = 13.sp,
            color = TextSecondary,
            modifier = Modifier.padding(top = 2.dp, bottom = 12.dp)
        )

        PubgDivider(modifier = Modifier.padding(bottom = 16.dp))

        // Card 1: Usage Access Permission
        GlassCard(
            borderColor = if (hasUsageAccess) AccentGreen else AccentRed
        ) {
            Text(
                text = "1. Android Usage Access",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Android protects foreground app statistics. To automatically detect when you start PUBG Mobile without constantly consuming battery, Usage Access is required.",
                fontSize = 12.sp,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (hasUsageAccess) "✅ Usage Access Enabled" else "❌ Usage Access Not Enabled",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (hasUsageAccess) AccentGreen else AccentRed
                )

                if (!hasUsageAccess) {
                    SecondaryButton(
                        text = "Enable Access",
                        onClick = {
                            PubgDetector.openUsageStatsSettings(context)
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Card 2: PUBG Mobile App Status
        GlassCard {
            Text(
                text = "2. PUBG Mobile Package Status",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Supported variants: Global (com.tencent.ig), Korea (com.pubg.krmobile), India BGMI (com.pubg.imobile), Vietnam, and HD.",
                fontSize = 12.sp,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = if (isPubgInstalled) "✅ PUBG Mobile Detected on Device" else "ℹ️ PUBG Mobile Not Installed (Test Mode Available Below)",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (isPubgInstalled) AccentGreen else AccentYellow
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Card 3: Interactive Simulation / Test Mode
        GlassCard(
            borderColor = if (isSimModeEnabled) AccentYellow else BorderDark
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "3. Interactive Test / Simulation Mode",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = TextPrimary
                    )
                    Text(
                        text = "Allows simulating PUBG Mobile launching and closing directly from this screen to test notifications without running the actual game.",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }

                Switch(
                    checked = isSimModeEnabled,
                    onCheckedChange = {
                        isSimModeEnabled = it
                        viewModel.sessionManager.isSimulationMode = it
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = AccentGreen,
                        checkedTrackColor = CardHover
                    )
                )
            }

            if (isSimModeEnabled) {
                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isSimulatedActive) "🟢 State: PLAYING_PUBG" else "⚫ State: OFFLINE",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSimulatedActive) AccentGreen else TextSecondary
                    )

                    PrimaryButton(
                        text = if (isSimulatedActive) "Simulate Close PUBG" else "Simulate Open PUBG",
                        onClick = { viewModel.toggleSimulatedPubg() },
                        modifier = Modifier.width(180.dp)
                    )
                }
            }
        }
    }
}
