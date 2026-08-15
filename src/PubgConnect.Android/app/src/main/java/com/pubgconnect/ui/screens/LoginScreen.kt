package com.pubgconnect.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pubgconnect.api.ApiClient
import com.pubgconnect.ui.components.GlassCard
import com.pubgconnect.ui.components.ModernTextField
import com.pubgconnect.ui.components.PrimaryButton
import com.pubgconnect.ui.components.SecondaryButton
import com.pubgconnect.ui.theme.*
import com.pubgconnect.ui.viewmodels.MainViewModel

@Composable
fun LoginScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    var isLoginTab by remember { mutableStateOf(true) }

    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("ali@pubg.com") }
    var password by remember { mutableStateOf("password123") }
    var serverUrl by remember { mutableStateOf(viewModel.sessionManager.serverUrl) }
    var showServerConfig by remember { mutableStateOf(false) }

    val isLoading by viewModel.isLoading.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDark)
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // App Branding Header
        Text(text = "🎮", fontSize = 48.sp)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "PUBG CONNECT",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
        Text(
            text = "GameLoop & Android Friend Notifier",
            fontSize = 13.sp,
            color = TextSecondary,
            modifier = Modifier.padding(top = 4.dp, bottom = 24.dp)
        )

        // Login / Register Tabs
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(CardBg, RoundedCornerShape(8.dp))
                .padding(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .background(
                        if (isLoginTab) AccentGreen else Color.Transparent,
                        RoundedCornerShape(6.dp)
                    )
                    .clickable { isLoginTab = true }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Sign In",
                    fontWeight = FontWeight.SemiBold,
                    color = if (isLoginTab) Color.White else TextSecondary,
                    fontSize = 14.sp
                )
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .background(
                        if (!isLoginTab) AccentGreen else Color.Transparent,
                        RoundedCornerShape(6.dp)
                    )
                    .clickable { isLoginTab = false }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Register",
                    fontWeight = FontWeight.SemiBold,
                    color = if (!isLoginTab) Color.White else TextSecondary,
                    fontSize = 14.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Form Fields
        GlassCard {
            if (!isLoginTab) {
                ModernTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = "Username",
                    placeholder = "Enter your display name"
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            ModernTextField(
                value = email,
                onValueChange = { email = it },
                label = "Email Address",
                placeholder = "name@example.com"
            )

            Spacer(modifier = Modifier.height(12.dp))

            ModernTextField(
                value = password,
                onValueChange = { password = it },
                label = "Password",
                placeholder = "Enter password",
                visualTransformation = PasswordVisualTransformation()
            )

            Spacer(modifier = Modifier.height(20.dp))

            PrimaryButton(
                text = if (isLoginTab) "Sign In" else "Create Account",
                onClick = {
                    if (isLoginTab) {
                        viewModel.login(email, password) { success, msg ->
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        viewModel.register(username, email, password) { success, msg ->
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                isLoading = isLoading
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Quick Demo Fill Accounts Card
        GlassCard(backgroundColor = CardBg) {
            Text(
                text = "⚡ Quick Demo Accounts (Tap to Fill)",
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = AccentYellow,
                modifier = Modifier.padding(bottom = 10.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SecondaryButton(
                    text = "Ali (PC)",
                    onClick = {
                        email = "ali@pubg.com"
                        password = "password123"
                        isLoginTab = true
                    },
                    modifier = Modifier.weight(1f)
                )

                SecondaryButton(
                    text = "Ahmed (Mob)",
                    onClick = {
                        email = "ahmed@pubg.com"
                        password = "password123"
                        isLoginTab = true
                    },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SecondaryButton(
                    text = "Hassan",
                    onClick = {
                        email = "hassan@pubg.com"
                        password = "password123"
                        isLoginTab = true
                    },
                    modifier = Modifier.weight(1f)
                )

                SecondaryButton(
                    text = "Usman",
                    onClick = {
                        email = "usman@pubg.com"
                        password = "password123"
                        isLoginTab = true
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Server URL toggle
        Text(
            text = if (showServerConfig) "▲ Hide Server URL" else "▼ Configure Server Host ($serverUrl)",
            color = TextSecondary,
            fontSize = 12.sp,
            modifier = Modifier
                .clickable { showServerConfig = !showServerConfig }
                .padding(8.dp)
        )

        if (showServerConfig) {
            GlassCard(modifier = Modifier.padding(top = 8.dp)) {
                ModernTextField(
                    value = serverUrl,
                    onValueChange = { serverUrl = it },
                    label = "Backend Server URL",
                    placeholder = "http://10.0.2.2:5000"
                )
                Spacer(modifier = Modifier.height(8.dp))
                SecondaryButton(
                    text = "Apply Server URL",
                    onClick = {
                        viewModel.sessionManager.serverUrl = serverUrl
                        ApiClient.updateBaseUrl(serverUrl)
                        Toast.makeText(context, "Server URL updated!", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }
    }
}
