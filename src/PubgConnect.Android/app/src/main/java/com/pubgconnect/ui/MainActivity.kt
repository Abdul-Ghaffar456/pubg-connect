package com.pubgconnect.ui

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
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
    REQUESTS("Requests", Icons.Default.Add),
    SETTINGS("Settings", Icons.Default.Settings)
}

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    private val requestNotificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            // Notification permission handled
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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
    val context = LocalContext.current
    var currentScreen by remember { mutableStateOf(NavigationScreen.FRIENDS) }
    val pendingRequests by viewModel.pendingRequests.collectAsState()
    val hasUsageAccess by viewModel.hasUsageAccess.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(text = "🎮", fontSize = 20.sp)
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
                            shape = RoundedCornerShape(16.dp),
                            color = CardHover,
                            modifier = Modifier
                                .padding(end = 12.dp)
                                .clickable {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val clip = ClipData.newPlainText("Friend ID", user.friendId)
                                    clipboard.setPrimaryClip(clip)
                                    Toast.makeText(context, "Friend ID copied: ${user.friendId}", Toast.LENGTH_SHORT).show()
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "ID: ",
                                    fontSize = 11.sp,
                                    color = TextSecondary
                                )
                                Text(
                                    text = user.friendId,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AccentGreen
                                )
                            }
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
                contentColor = TextPrimary,
                tonalElevation = 8.dp
            ) {
                NavigationScreen.entries.forEach { screen ->
                    val isSelected = currentScreen == screen

                    NavigationBarItem(
                        icon = {
                            BadgedBox(
                                badge = {
                                    if (screen == NavigationScreen.REQUESTS && pendingRequests.isNotEmpty()) {
                                        Badge(containerColor = AccentGreen) {
                                            Text(text = "${pendingRequests.size}", color = Color.White, fontSize = 10.sp)
                                        }
                                    } else if (screen == NavigationScreen.SETTINGS && !hasUsageAccess) {
                                        Badge(containerColor = AccentYellow)
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = screen.icon,
                                    contentDescription = screen.label,
                                    tint = if (isSelected) AccentGreen else TextSecondary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        },
                        label = {
                            Text(
                                text = screen.label,
                                fontSize = 12.sp,
                                color = if (isSelected) AccentGreen else TextSecondary,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
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
                NavigationScreen.FRIENDS -> FriendsScreen(
                    viewModel = viewModel,
                    onNavigateToAddFriend = { currentScreen = NavigationScreen.REQUESTS }
                )
                NavigationScreen.ACTIVITY -> ActivityScreen(viewModel = viewModel)
                NavigationScreen.REQUESTS -> RequestsScreen(viewModel = viewModel)
                NavigationScreen.SETTINGS -> SettingsScreen(viewModel = viewModel)
            }
        }
    }
}
