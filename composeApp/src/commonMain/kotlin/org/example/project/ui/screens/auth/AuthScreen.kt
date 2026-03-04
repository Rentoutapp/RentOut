package org.example.project.ui.screens.auth

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import org.example.project.presentation.AuthEvent
import org.example.project.presentation.AuthState
import org.example.project.presentation.AuthViewModel
import org.example.project.ui.components.*
import org.example.project.ui.theme.RentOutColors
import org.example.project.ui.util.ImagePickerSource
import org.example.project.ui.util.PickedImage
import org.example.project.ui.util.rememberImagePickerLauncher

// ─── Country code data ────────────────────────────────────────────────────────
private data class CountryCode(val flag: String, val name: String, val code: String, val hint: String)
private val countryCodes = listOf(
    CountryCode("🇿🇼", "Zimbabwe",     "+263", "7XX XXX XXX"),
    CountryCode("🇿🇦", "South Africa", "+27",  "7X XXX XXXX"),
    CountryCode("🇧🇼", "Botswana",     "+267", "7X XXX XXX"),
    CountryCode("🇿🇲", "Zambia",       "+260", "9X XXX XXXX"),
    CountryCode("🇲🇿", "Mozambique",   "+258", "8X XXX XXXX"),
    CountryCode("🇳🇦", "Namibia",      "+264", "8X XXX XXXX"),
    CountryCode("🇺🇸", "USA",          "+1",   "XXX XXX XXXX"),
    CountryCode("🇬🇧", "UK",           "+44",  "7XXX XXX XXX"),
)

@Composable
fun AuthScreen(
    selectedRole: String,
    authState: AuthState,
    onLogin: (String, String, Boolean) -> Unit,
    onRegister: (String, String, String, String, String, ByteArray?) -> Unit,
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
    var regName            by remember { mutableStateOf("") }
    var regEmail           by remember { mutableStateOf("") }
    var regPhone           by remember { mutableStateOf("") }
    var regCountry         by remember { mutableStateOf(countryCodes[0]) }
    var regPassword        by remember { mutableStateOf("") }
    var regConfirm         by remember { mutableStateOf("") }
    var regPhotoUri        by remember { mutableStateOf("") }   // local URI string
    var regPhotoBytes      by remember { mutableStateOf<ByteArray?>(null) } // raw bytes for upload
    var showPhotoDialog    by remember { mutableStateOf(false) }
    var showCountryPicker  by remember { mutableStateOf(false) }

    // Validation errors — declared BEFORE imagePicker so the lambda can reference them
    var nameError     by remember { mutableStateOf("") }
    var emailError    by remember { mutableStateOf("") }
    var phoneError    by remember { mutableStateOf("") }
    var photoError    by remember { mutableStateOf("") }
    var passwordError by remember { mutableStateOf("") }
    var confirmError  by remember { mutableStateOf("") }

    // Real platform image picker
    val imagePicker = rememberImagePickerLauncher { picked: PickedImage? ->
        if (picked != null) {
            regPhotoUri   = picked.uri
            regPhotoBytes = picked.bytes
            photoError    = ""
        }
    }

    val isLoading  = authState is AuthState.Loading
    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()

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

                        // ── 1. Full Name ──────────────────────────────────────
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

                        // ── 2. Email ──────────────────────────────────────────
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

                        // ── 3. Phone Number with country code ─────────────────
                        Column {
                            Text(
                                "Phone Number",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (phoneError.isNotEmpty()) MaterialTheme.colorScheme.error
                                        else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Country code selector pill
                                Surface(
                                    modifier = Modifier
                                        .height(56.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .clickable { showCountryPicker = true }
                                        .border(
                                            width = if (phoneError.isNotEmpty()) 1.5.dp else 1.dp,
                                            color = if (phoneError.isNotEmpty()) MaterialTheme.colorScheme.error
                                                    else MaterialTheme.colorScheme.outline,
                                            shape = RoundedCornerShape(14.dp)
                                        ),
                                    color = MaterialTheme.colorScheme.surface,
                                    shape = RoundedCornerShape(14.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(regCountry.flag, fontSize = 20.sp)
                                        Text(
                                            regCountry.code,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 14.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Icon(
                                            Icons.Default.ArrowDropDown, null,
                                            modifier = Modifier.size(18.dp),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                // Phone number input
                                RentOutTextField(
                                    value = regPhone,
                                    onValueChange = { regPhone = it; phoneError = "" },
                                    label = regCountry.hint,
                                    leadingIcon = Icons.Default.Phone,
                                    leadingIconTint = RentOutColors.IconGreen,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                    isError = phoneError.isNotEmpty(),
                                    errorMessage = "",
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            AnimatedVisibility(visible = phoneError.isNotEmpty()) {
                                Text(
                                    phoneError,
                                    color = MaterialTheme.colorScheme.error,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(start = 4.dp, top = 4.dp)
                                )
                            }
                        }
                        Spacer(Modifier.height(14.dp))

                        // ── 4. Profile Photo ──────────────────────────────────
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Centered heading
                            Text(
                                "Profile Photo",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                textAlign = TextAlign.Center,
                                color = if (photoError.isNotEmpty()) MaterialTheme.colorScheme.error
                                        else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(bottom = 10.dp)
                            )

                            // Avatar shell — centered, slightly larger
                            Box(contentAlignment = Alignment.BottomEnd) {
                                Box(
                                    modifier = Modifier
                                        .size(88.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (regPhotoUri.isNotEmpty())
                                                Brush.linearGradient(listOf(RentOutColors.Secondary, RentOutColors.SecondaryLight))
                                            else
                                                Brush.linearGradient(listOf(
                                                    MaterialTheme.colorScheme.surfaceVariant,
                                                    MaterialTheme.colorScheme.surfaceVariant
                                                ))
                                        )
                                        .border(
                                            width = if (photoError.isNotEmpty()) 2.dp else 1.5.dp,
                                            color = if (photoError.isNotEmpty()) MaterialTheme.colorScheme.error
                                                    else if (regPhotoUri.isNotEmpty()) RentOutColors.Secondary
                                                    else MaterialTheme.colorScheme.outline,
                                            shape = CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (regPhotoUri.isNotEmpty()) {
                                        val initials = regName.split(" ").filter { it.isNotBlank() }
                                            .take(2).joinToString("") { it.first().uppercaseChar().toString() }.ifBlank { "?" }
                                        Text(initials, color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                                    } else {
                                        Icon(
                                            Icons.Default.Person, null,
                                            modifier = Modifier.size(38.dp),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                // Camera icon badge — sits at bottom-right of the avatar
                                val camInteraction = remember { MutableInteractionSource() }
                                val isCamPressed by camInteraction.collectIsPressedAsState()
                                val camScale by animateFloatAsState(
                                    targetValue = if (isCamPressed) 0.85f else 1f,
                                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                                    label = "cam_scale"
                                )
                                Box(
                                    modifier = Modifier
                                        .size(30.dp)
                                        .scale(camScale)
                                        .clip(CircleShape)
                                        .background(RentOutColors.Primary)
                                        .clickable(
                                            interactionSource = camInteraction,
                                            indication = null
                                        ) { showPhotoDialog = true; photoError = "" },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.CameraAlt, null,
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }

                            Spacer(Modifier.height(8.dp))

                            // Status text below avatar
                            if (regPhotoUri.isNotEmpty()) {
                                Text(
                                    "✓ Photo selected",
                                    color = RentOutColors.StatusApproved,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    textAlign = TextAlign.Center
                                )
                            } else {
                                Text(
                                    "Tap the camera icon to add a photo",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 11.sp,
                                    textAlign = TextAlign.Center
                                )
                            }

                            AnimatedVisibility(visible = photoError.isNotEmpty()) {
                                Text(
                                    photoError,
                                    color = MaterialTheme.colorScheme.error,
                                    fontSize = 11.sp,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }
                        Spacer(Modifier.height(14.dp))

                        // ── 5. Password ───────────────────────────────────────
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

                        // ── 6. Confirm Password ───────────────────────────────
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

                        // ── Create Account button with intelligent validation ──
                        RentOutPrimaryButton(
                            text = "Create Account",
                            onClick = {
                                // Reset all errors
                                nameError = ""; emailError = ""; phoneError = ""
                                photoError = ""; passwordError = ""; confirmError = ""

                                // Validate all fields in order; track first error for scroll
                                data class FieldError(val setter: (String) -> Unit, val msg: String, val scrollY: Int)
                                val errors = mutableListOf<FieldError>()

                                if (regName.isBlank())
                                    errors += FieldError({ nameError = it }, "Full name is required", 0)
                                if (regEmail.isBlank())
                                    errors += FieldError({ emailError = it }, "Email address is required", 80)
                                else if (!regEmail.contains("@") || !regEmail.contains("."))
                                    errors += FieldError({ emailError = it }, "Enter a valid email address", 80)
                                if (regPhone.isBlank())
                                    errors += FieldError({ phoneError = it }, "Phone number is required", 180)
                                else if (regPhone.replace(" ", "").length < 7)
                                    errors += FieldError({ phoneError = it }, "Enter a valid phone number", 180)
                                if (regPhotoUri.isEmpty())
                                    errors += FieldError({ photoError = it }, "A profile photo is required", 300)
                                if (regPassword.length < 6)
                                    errors += FieldError({ passwordError = it }, "Password must be at least 6 characters", 420)
                                if (regPassword != regConfirm)
                                    errors += FieldError({ confirmError = it }, "Passwords do not match", 500)

                                if (errors.isNotEmpty()) {
                                    // Apply all errors so user sees every issue at once
                                    errors.forEach { it.setter(it.msg) }
                                    // Scroll to the first error
                                    coroutineScope.launch {
                                        scrollState.animateScrollTo(errors.first().scrollY)
                                    }
                                } else {
                                    val fullPhone = "${regCountry.code} ${regPhone.trim()}"
                                    onRegister(regName.trim(), regEmail.trim(), regPassword, fullPhone, regPhotoUri, regPhotoBytes)
                                }
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

    // ── Photo source dialog ───────────────────────────────────────────────────
    if (showPhotoDialog) {
        AlertDialog(
            onDismissRequest = { showPhotoDialog = false },
            icon = { Icon(Icons.Default.CameraAlt, null, tint = RentOutColors.Primary) },
            title = { Text("Add Profile Photo", fontWeight = FontWeight.Bold) },
            text = { Text("Choose how you'd like to add your photo.") },
            confirmButton = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            showPhotoDialog = false
                            imagePicker.launch(ImagePickerSource.CAMERA)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = RentOutColors.Primary)
                    ) {
                        Icon(Icons.Default.CameraAlt, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Take a Photo")
                    }
                    Button(
                        onClick = {
                            showPhotoDialog = false
                            imagePicker.launch(ImagePickerSource.GALLERY)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = RentOutColors.Secondary)
                    ) {
                        Icon(Icons.Default.PhotoLibrary, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Browse Gallery")
                    }
                    TextButton(
                        onClick = { showPhotoDialog = false },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Cancel") }
                }
            },
            dismissButton = {},
            shape = RoundedCornerShape(20.dp)
        )
    }

    // ── Country code picker dialog ────────────────────────────────────────────
    if (showCountryPicker) {
        AlertDialog(
            onDismissRequest = { showCountryPicker = false },
            title = { Text("Select Country", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    countryCodes.forEach { country ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    regCountry = country
                                    regPhone = ""
                                    showCountryPicker = false
                                }
                                .background(
                                    if (regCountry == country) RentOutColors.Primary.copy(alpha = 0.08f)
                                    else Color.Transparent
                                )
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(country.flag, fontSize = 22.sp)
                            Text(country.name, modifier = Modifier.weight(1f), fontWeight = FontWeight.Medium)
                            Text(
                                country.code,
                                color = RentOutColors.Primary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showCountryPicker = false }) { Text("Cancel") }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }
}

