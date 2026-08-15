package com.pubgconnect.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.pubgconnect.detection.PubgDetectionService
import com.pubgconnect.ui.screens.*
import com.pubgconnect.ui.theme.*
import com.pubgconnect.ui.viewmodels.MainViewModel

enum class NavigationScreen(val label: String, val icon: ImageVector) {
    FRIENDS("Friends", Icons.Default.Person),
    ACTIVITY("Activity", Icons.Default.Star),
    ADD_FRIEND("Add", Icons.Default.Add),
    REQUESTS("Requests", Icons.Default.Email),
    DETECTION("Detection", Icons.Default.PlayArrow),
    SETTINGS("Settings", Icons.Default.Settings)
}

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    private val requestNotificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            // Permission granted or rejected
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Request POST_NOTIFICATIONS on Android 13+ (Tiramisu)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        setContent {
            PubgConnectTheme {
                MainAppHost(viewModel = viewModel)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshUsageAccessStatus()
        if (viewModel.isLoggedIn.value) {
            viewModel.loadFriends()
            viewModel.loadPendingRequests()
            viewModel.loadActivity()
            PubgDetectionService.start(this)
        }
    }
}

@Composable
fun MainAppHost(viewModel: MainViewModel) {
    val isLoggedIn by viewModel.isLoggedIn.collectAsState()

    if (!isLoggedIn) {
        LoginScreen(viewModel = viewModel)
    } else {
        MainScreenWithNav(viewModel = viewModel)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreenWithNav(viewModel: MainViewModel) {
    var currentScreen by remember { mutableStateOf(NavigationScreen.FRIENDS) }
    val pendingRequests by viewModel.pendingRequests.collectAsState()
    val hasUsageAccess by viewModel.hasUsageAccess.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "🎮", fontSize = 18.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "PUBG CONNECT",
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            color = TextPrimary
                        )
                    }
                },
                actions = {
                    currentUser?.let { user ->
                        Surface(
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                            color = CardHover,
                            modifier = Modifier.padding(end = 12.dp)
                        ) {
                            Text(
                                text = "ID: ${user.friendId}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = AccentGreen,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF0A0D15),
                    titleContentColor = TextPrimary
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = Color(0xFF0A0D15),
                contentColor = TextPrimary
            ) {
                NavigationScreen.entries.forEach { screen ->
                    val isSelected = currentScreen == screen

                    NavigationBarItem(
                        icon = {
                            BadgedBox(
                                badge = {
                                    if (screen == NavigationScreen.REQUESTS && pendingRequests.isNotEmpty()) {
                                        Badge(containerColor = AccentGreen) {
                                            Text(text = "${pendingRequests.size}", color = Color.White)
                                        }
                                    } else if (screen == NavigationScreen.DETECTION && !hasUsageAccess) {
                                        Badge(containerColor = AccentRed)
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = screen.icon,
                                    contentDescription = screen.label,
                                    tint = if (isSelected) AccentGreen else TextSecondary
                                )
                            }
                        },
                        label = {
                            Text(
                                text = screen.label,
                                fontSize = 11.sp,
                                color = if (isSelected) AccentGreen else TextSecondary,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        selected = isSelected,
                        onClick = { currentScreen = screen },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = AccentGreen,
                            unselectedIconColor = TextSecondary,
                            selectedTextColor = AccentGreen,
                            unselectedTextColor = TextSecondary,
                            indicatorColor = CardHover
                        )
                    )
                }
            }
        },
        containerColor = BgDark
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (currentScreen) {
                NavigationScreen.FRIENDS -> FriendsScreen(viewModel = viewModel)
                NavigationScreen.ACTIVITY -> ActivityScreen(viewModel = viewModel)
                NavigationScreen.ADD_FRIEND -> AddFriendScreen(viewModel = viewModel)
                NavigationScreen.REQUESTS -> RequestsScreen(viewModel = viewModel)
                NavigationScreen.DETECTION -> DetectionSetupScreen(viewModel = viewModel)
                NavigationScreen.SETTINGS -> SettingsScreen(viewModel = viewModel)
            }
        }
    }
}
