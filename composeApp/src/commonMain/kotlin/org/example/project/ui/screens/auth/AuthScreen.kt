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
import androidx.compose.ui.focus.onFocusChanged
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.compose.AsyncImage
import kotlinx.coroutines.delay
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
    initialTab: Int = 0,
    registrationProgress: Float = 0f,
    registrationStep: String = ""
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
                        val photoShape = RoundedCornerShape(20.dp)
                        val photoWidth  = 120.dp
                        val photoHeight = 150.dp

                        // Track password field focus — halo pauses when user moves to passwords
                        var passwordFocused by remember { mutableStateOf(false) }
                        var confirmFocused  by remember { mutableStateOf(false) }
                        val passwordSectionActive = passwordFocused || confirmFocused

                        // Halo animation — infinite pulse, but smoothly gated by passwordSectionActive.
                        // When password fields are focused, haloActive snaps to 0 via animateFloatAsState,
                        // which multiplies the infinite alpha/spread to effectively freeze the halo at 0.
                        val haloTransition = rememberInfiniteTransition(label = "halo")
                        val haloAlphaRaw by haloTransition.animateFloat(
                            initialValue = 0.35f,
                            targetValue  = 0.85f,
                            animationSpec = infiniteRepeatable(
                                tween(1400, easing = EaseInOutSine),
                                RepeatMode.Reverse
                            ),
                            label = "halo_alpha"
                        )
                        val haloSpreadRaw by haloTransition.animateFloat(
                            initialValue = 0f,
                            targetValue  = 6f,
                            animationSpec = infiniteRepeatable(
                                tween(1400, easing = EaseInOutSine),
                                RepeatMode.Reverse
                            ),
                            label = "halo_spread"
                        )
                        // Gate — smoothly fades halo in/out as password section gains/loses focus
                        val haloGate by animateFloatAsState(
                            targetValue = if (passwordSectionActive) 0f else 1f,
                            animationSpec = tween(400, easing = EaseInOutSine),
                            label = "halo_gate"
                        )
                        val haloAlpha  = haloAlphaRaw  * haloGate
                        val haloSpread = haloSpreadRaw * haloGate

                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                "Profile Photo",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                textAlign = TextAlign.Center,
                                color = if (photoError.isNotEmpty()) MaterialTheme.colorScheme.error
                                        else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(bottom = 10.dp)
                            )

                            Box(contentAlignment = Alignment.BottomEnd) {

                                // Halo ring — only shown when photo is loaded
                                if (regPhotoUri.isNotEmpty()) {
                                    Box(
                                        modifier = Modifier
                                            .width(photoWidth + haloSpread.dp * 2)
                                            .height(photoHeight + haloSpread.dp * 2)
                                            .border(
                                                width = 1.dp,
                                                color = RentOutColors.Secondary.copy(alpha = haloAlpha),
                                                shape = RoundedCornerShape(20.dp + haloSpread.dp)
                                            )
                                    )
                                }

                                // Image shell — vertical rounded rectangle
                                Box(
                                    modifier = Modifier
                                        .width(photoWidth)
                                        .height(photoHeight)
                                        .clip(photoShape)
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
                                            width = if (photoError.isNotEmpty()) 2.dp else 1.dp,
                                            color = if (photoError.isNotEmpty()) MaterialTheme.colorScheme.error
                                                    else if (regPhotoUri.isNotEmpty()) RentOutColors.Secondary
                                                    else MaterialTheme.colorScheme.outline,
                                            shape = photoShape
                                        )
                                        .clickable(
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = null
                                        ) { showPhotoDialog = true; photoError = "" },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (regPhotoUri.isNotEmpty()) {
                                        AsyncImage(
                                            model = regPhotoUri,
                                            contentDescription = "Profile photo",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .width(photoWidth)
                                                .height(photoHeight)
                                                .clip(photoShape)
                                        )
                                    } else {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Person, null,
                                                modifier = Modifier.size(44.dp),
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                            )
                                            Text(
                                                "Tap to add\nphoto",
                                                fontSize = 11.sp,
                                                textAlign = TextAlign.Center,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                            )
                                        }
                                    }
                                }

                                // Camera badge — bottom-right corner of the rectangle
                                val camInteraction = remember { MutableInteractionSource() }
                                val isCamPressed by camInteraction.collectIsPressedAsState()
                                val camScale by animateFloatAsState(
                                    targetValue = if (isCamPressed) 0.85f else 1f,
                                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                                    label = "cam_scale"
                                )
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
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
                                        modifier = Modifier.size(17.dp)
                                    )
                                }
                            }

                            Spacer(Modifier.height(10.dp))

                            AnimatedContent(
                                targetState = regPhotoUri.isNotEmpty(),
                                transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(200)) },
                                label = "photo_status"
                            ) { hasPhoto ->
                                if (hasPhoto) {
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
                            errorMessage = passwordError,
                            modifier = Modifier.onFocusChanged { passwordFocused = it.isFocused }
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
                            errorMessage = confirmError,
                            modifier = Modifier.onFocusChanged { confirmFocused = it.isFocused }
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

    // ── Registration loading overlay ──────────────────────────────────────────
    val isRegistering = authState is AuthState.Loading && registrationProgress > 0f
    AnimatedVisibility(
        visible = isRegistering,
        enter = fadeIn(tween(300)),
        exit = fadeOut(tween(400))
    ) {
        Dialog(
            onDismissRequest = {},
            properties = DialogProperties(
                dismissOnBackPress = false,
                dismissOnClickOutside = false,
                usePlatformDefaultWidth = false
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.72f)),
                contentAlignment = Alignment.Center
            ) {
                // Animated progress card
                val animatedProgress by animateFloatAsState(
                    targetValue = registrationProgress,
                    animationSpec = tween(durationMillis = 600, easing = EaseInOutCubic),
                    label = "reg_progress"
                )

                // Pulsing glow ring behind card
                val pulse = rememberInfiniteTransition(label = "pulse")
                val pulseScale by pulse.animateFloat(
                    initialValue = 0.95f, targetValue = 1.05f,
                    animationSpec = infiniteRepeatable(
                        tween(900, easing = EaseInOutSine),
                        RepeatMode.Reverse
                    ),
                    label = "pulse_scale"
                )

                Box(
                    modifier = Modifier
                        .size(260.dp)
                        .scale(pulseScale)
                        .background(
                            Brush.radialGradient(
                                listOf(
                                    RentOutColors.Primary.copy(alpha = 0.25f),
                                    Color.Transparent
                                )
                            ),
                            shape = CircleShape
                        )
                )

                Card(
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .wrapContentHeight(),
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 24.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        // Spinning arc indicator
                        val rotation by pulse.animateFloat(
                            initialValue = 0f, targetValue = 360f,
                            animationSpec = infiniteRepeatable(tween(1200, easing = LinearEasing)),
                            label = "spinner_rot"
                        )
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .graphicsLayer { rotationZ = rotation },
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(64.dp),
                                color = RentOutColors.Primary,
                                strokeWidth = 5.dp
                            )
                            Icon(
                                Icons.Default.Person,
                                null,
                                tint = RentOutColors.Primary,
                                modifier = Modifier.size(28.dp)
                                    .graphicsLayer { rotationZ = -rotation } // counter-rotate so icon stays upright
                            )
                        }

                        // Step label — animated crossfade between steps
                        AnimatedContent(
                            targetState = registrationStep,
                            transitionSpec = {
                                fadeIn(tween(300)) + slideInVertically { it / 2 } togetherWith
                                fadeOut(tween(200))
                            },
                            label = "step_label"
                        ) { step ->
                            Text(
                                text = step.ifBlank { "Setting things up…" },
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center
                            )
                        }

                        // Progress bar
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            LinearProgressIndicator(
                                progress = animatedProgress,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(50)),
                                color = RentOutColors.Primary,
                                trackColor = RentOutColors.Primary.copy(alpha = 0.15f)
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    "Progress",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    "${(animatedProgress * 100).toInt()}%",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = RentOutColors.Primary
                                )
                            }
                        }
                    }
                }
            }
        }
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

