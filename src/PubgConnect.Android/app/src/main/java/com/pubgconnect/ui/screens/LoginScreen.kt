package com.pubgconnect.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pubgconnect.ui.components.GlassCard
import com.pubgconnect.ui.components.ModernTextField
import com.pubgconnect.ui.components.PrimaryButton
import com.pubgconnect.ui.theme.*
import com.pubgconnect.ui.viewmodels.MainViewModel

@Composable
fun LoginScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    var isLoginTab by remember { mutableStateOf(true) }

    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    val isLoading by viewModel.isLoading.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDark)
            .imePadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 22.dp, vertical = 24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.height(20.dp))

            // Big App Brand Hero Icon
            Box(
                modifier = Modifier
                    .size(90.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF132822))
                    .border(2.dp, AccentGreen, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "🎮", fontSize = 48.sp)
            }

            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = "PUBG CONNECT",
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                letterSpacing = 1.sp
            )
            Text(
                text = "GameLoop & Android Friend Notifier",
                fontSize = 13.sp,
                color = TextSecondary,
                modifier = Modifier.padding(top = 4.dp, bottom = 24.dp)
            )

            // Login / Register Switch Tabs
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CardBg, RoundedCornerShape(12.dp))
                    .padding(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(
                            if (isLoginTab) AccentGreen else Color.Transparent,
                            RoundedCornerShape(10.dp)
                        )
                        .clickable { isLoginTab = true }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Sign In",
                        fontWeight = FontWeight.Bold,
                        color = if (isLoginTab) Color.White else TextSecondary,
                        fontSize = 14.sp
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(
                            if (!isLoginTab) AccentGreen else Color.Transparent,
                            RoundedCornerShape(10.dp)
                        )
                        .clickable { isLoginTab = false }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Register",
                        fontWeight = FontWeight.Bold,
                        color = if (!isLoginTab) Color.White else TextSecondary,
                        fontSize = 14.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Form Fields Card
            GlassCard {
                if (!isLoginTab) {
                    ModernTextField(
                        value = username,
                        onValueChange = { username = it },
                        label = "Display Name",
                        placeholder = "e.g. ShadowHunter",
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                }

                ModernTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = "Email Address",
                    placeholder = "name@example.com",
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Next
                    )
                )

                Spacer(modifier = Modifier.height(14.dp))

                ModernTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = "Password",
                    placeholder = "Enter your password",
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(onDone = {
                        focusManager.clearFocus()
                        if (isLoginTab) {
                            if (email.isBlank() || password.isBlank()) {
                                Toast.makeText(context, "Please enter your email and password", Toast.LENGTH_SHORT).show()
                                return@KeyboardActions
                            }
                            viewModel.login(email, password) { success, msg ->
                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            if (username.isBlank() || email.isBlank() || password.isBlank()) {
                                Toast.makeText(context, "Please fill in all fields", Toast.LENGTH_SHORT).show()
                                return@KeyboardActions
                            }
                            viewModel.register(username, email, password) { success, msg ->
                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                            }
                        }
                    })
                )

                Spacer(modifier = Modifier.height(22.dp))

                PrimaryButton(
                    text = if (isLoginTab) "Sign In" else "Create Account",
                    onClick = {
                        focusManager.clearFocus()
                        if (isLoginTab) {
                            if (email.isBlank() || password.isBlank()) {
                                Toast.makeText(context, "Please enter your email and password", Toast.LENGTH_SHORT).show()
                                return@PrimaryButton
                            }
                            viewModel.login(email, password) { success, msg ->
                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            if (username.isBlank() || email.isBlank() || password.isBlank()) {
                                Toast.makeText(context, "Please fill in all fields", Toast.LENGTH_SHORT).show()
                                return@PrimaryButton
                            }
                            viewModel.register(username, email, password) { success, msg ->
                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    isLoading = isLoading
                )
            }

            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}
