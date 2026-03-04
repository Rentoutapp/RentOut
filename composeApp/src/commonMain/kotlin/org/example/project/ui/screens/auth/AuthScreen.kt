package org.example.project.ui.screens.auth

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.example.project.presentation.AuthEvent
import org.example.project.presentation.AuthState
import org.example.project.presentation.AuthViewModel
import org.example.project.ui.components.*
import org.example.project.ui.theme.RentOutColors

@Composable
fun AuthScreen(
    selectedRole: String,
    authState: AuthState,
    onLogin: (String, String, Boolean) -> Unit,
    onRegister: (String, String, String) -> Unit,
    onBack: () -> Unit,
    onClearError: () -> Unit,
    prefillEmail: String = "",
    prefillPassword: String = "",
    initialTab: Int = 0
) {
    var selectedTab by remember { mutableStateOf(initialTab) }

    // Login fields — pre-filled when coming from registration
    var loginEmail    by remember { mutableStateOf(prefillEmail) }
    var loginPassword by remember { mutableStateOf(prefillPassword) }
    var rememberMe    by remember { mutableStateOf(false) }

    // Register fields
    var regName     by remember { mutableStateOf("") }
    var regEmail    by remember { mutableStateOf("") }
    var regPassword by remember { mutableStateOf("") }
    var regConfirm  by remember { mutableStateOf("") }

    // Validation errors
    var emailError    by remember { mutableStateOf("") }
    var passwordError by remember { mutableStateOf("") }
    var nameError     by remember { mutableStateOf("") }
    var confirmError  by remember { mutableStateOf("") }

    val isLoading = authState is AuthState.Loading
    val scrollState = rememberScrollState()

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        // Top gradient header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(RentOutColors.Primary, RentOutColors.Primary.copy(alpha = 0f))
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(56.dp))

            // Back button (mandatory animation)
            Row(modifier = Modifier.fillMaxWidth()) {
                var backPressed by remember { mutableStateOf(false) }
                val backScale by animateFloatAsState(
                    targetValue = if (backPressed) 0.8f else 1f,
                    animationSpec = tween(200),
                    label = "back_scale"
                )
                val backRotation by animateFloatAsState(
                    targetValue = if (backPressed) -45f else 0f,
                    animationSpec = tween(200),
                    label = "back_rotation"
                )
                IconButton(
                    onClick = {
                        backPressed = true
                        onBack()
                    },
                    modifier = Modifier.graphicsLayer {
                        scaleX = backScale
                        scaleY = backScale
                        rotationZ = backRotation
                    }
                ) {
                    Icon(Icons.Default.ArrowBack, "Back", tint = Color.White)
                }
            }

            Text(
                text = if (selectedRole == "landlord") "🏠 Landlord" else "🔑 Tenant",
                fontSize = 15.sp,
                color = Color.White.copy(alpha = 0.9f),
                fontWeight = FontWeight.Medium
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = if (selectedTab == 0) "Welcome back!" else "Create your account",
                fontSize = 26.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White
            )

            Spacer(Modifier.height(32.dp))

            // Tab selector
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(4.dp)
            ) {
                Row {
                    listOf("Login", "Register").forEachIndexed { index, label ->
                        val isActive = selectedTab == index
                        val tabScale by animateFloatAsState(
                            targetValue = if (isActive) 1f else 0.95f,
                            label = "tab_scale_$index"
                        )
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (isActive) MaterialTheme.colorScheme.primary
                                    else Color.Transparent
                                )
                                .clickable { selectedTab = index; onClearError() }
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                label,
                                color = if (isActive) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 15.sp
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // Error message
            AnimatedVisibility(authState is AuthState.Error) {
                val msg = (authState as? AuthState.Error)?.message ?: ""
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Error, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(msg, color = MaterialTheme.colorScheme.error, fontSize = 14.sp)
                    }
                }
            }

            // ── LOGIN TAB ──────────────────────────────────────────────────────
            AnimatedContent(
                targetState = selectedTab,
                transitionSpec = {
                    if (targetState > initialState)
                        slideInHorizontally { it } + fadeIn() togetherWith slideOutHorizontally { -it } + fadeOut()
                    else
                        slideInHorizontally { -it } + fadeIn() togetherWith slideOutHorizontally { it } + fadeOut()
                },
                label = "auth_tab_anim"
            ) { tab ->
                if (tab == 0) {
                    // LOGIN
                    Column(modifier = Modifier.fillMaxWidth()) {
                        RentOutTextField(
                            value = loginEmail,
                            onValueChange = { loginEmail = it; emailError = "" },
                            label = "Email Address",
                            leadingIcon = Icons.Default.Email,
                            leadingIconTint = RentOutColors.IconBlue,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            isError = emailError.isNotEmpty(),
                            errorMessage = emailError
                        )
                        Spacer(Modifier.height(16.dp))
                        RentOutTextField(
                            value = loginPassword,
                            onValueChange = { loginPassword = it; passwordError = "" },
                            label = "Password",
                            leadingIcon = Icons.Default.Lock,
                            leadingIconTint = RentOutColors.IconPurple,
                            isPassword = true,
                            isError = passwordError.isNotEmpty(),
                            errorMessage = passwordError
                        )
                        Spacer(Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = rememberMe,
                                onCheckedChange = { rememberMe = it },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = RentOutColors.Primary,
                                    uncheckedColor = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = "Remember me",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.clickable { rememberMe = !rememberMe }
                            )
                            Spacer(Modifier.weight(1f))
                            RentOutTextButton(
                                text = "Forgot Password?",
                                onClick = { /* TODO: password reset */ },
                                color = RentOutColors.Primary
                            )
                        }
                        Spacer(Modifier.height(24.dp))
                        RentOutPrimaryButton(
                            text = "Sign In",
                            onClick = {
                                var valid = true
                                if (loginEmail.isBlank()) { emailError = "Email is required"; valid = false }
                                if (loginPassword.isBlank()) { passwordError = "Password is required"; valid = false }
                                if (valid) onLogin(loginEmail.trim(), loginPassword, rememberMe)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            isLoading = isLoading
                        )
                    }
                } else {
                    // REGISTER
                    Column(modifier = Modifier.fillMaxWidth()) {
                        RentOutTextField(
                            value = regName,
                            onValueChange = { regName = it; nameError = "" },
                            label = "Full Name",
                            leadingIcon = Icons.Default.Person,
                            leadingIconTint = RentOutColors.IconTeal,
                            isError = nameError.isNotEmpty(),
                            errorMessage = nameError
                        )
                        Spacer(Modifier.height(14.dp))
                        RentOutTextField(
                            value = regEmail,
                            onValueChange = { regEmail = it; emailError = "" },
                            label = "Email Address",
                            leadingIcon = Icons.Default.Email,
                            leadingIconTint = RentOutColors.IconBlue,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            isError = emailError.isNotEmpty(),
                            errorMessage = emailError
                        )
                        Spacer(Modifier.height(14.dp))
                        RentOutTextField(
                            value = regPassword,
                            onValueChange = { regPassword = it; passwordError = "" },
                            label = "Password",
                            leadingIcon = Icons.Default.Lock,
                            leadingIconTint = RentOutColors.IconPurple,
                            isPassword = true,
                            isError = passwordError.isNotEmpty(),
                            errorMessage = passwordError
                        )
                        Spacer(Modifier.height(14.dp))
                        RentOutTextField(
                            value = regConfirm,
                            onValueChange = { regConfirm = it; confirmError = "" },
                            label = "Confirm Password",
                            leadingIcon = Icons.Default.Lock,
                            leadingIconTint = RentOutColors.IconSlate,
                            isPassword = true,
                            isError = confirmError.isNotEmpty(),
                            errorMessage = confirmError
                        )
                        Spacer(Modifier.height(24.dp))
                        RentOutPrimaryButton(
                            text = "Create Account",
                            onClick = {
                                var valid = true
                                if (regName.isBlank()) { nameError = "Name is required"; valid = false }
                                if (regEmail.isBlank()) { emailError = "Email is required"; valid = false }
                                if (regPassword.length < 6) { passwordError = "Password must be at least 6 characters"; valid = false }
                                if (regPassword != regConfirm) { confirmError = "Passwords do not match"; valid = false }
                                if (valid) onRegister(regName.trim(), regEmail.trim(), regPassword)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            isLoading = isLoading
                        )
                    }
                }
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}

