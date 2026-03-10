@file:OptIn(ExperimentalMaterial3Api::class)

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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.compose.AsyncImage
import kotlinx.coroutines.launch
import org.example.project.presentation.AuthEvent
import org.example.project.presentation.AuthState
import org.example.project.presentation.AuthViewModel
import org.example.project.ui.components.*
import org.example.project.ui.theme.RentOutColors
import org.example.project.ui.util.ImagePickerSource
import org.example.project.ui.util.PickedImage
import org.example.project.ui.util.rememberImagePickerLauncher

// ─── National ID auto-formatter ───────────────────────────────────────────────
/**
 * Formats raw input into Zimbabwe National ID format: ##-######[#] [A-Z]##
 *
 * Segment rules:
 *  1. Segment 1 — exactly 2 digits, followed by a hyphen '-'
 *  2. Segment 2 — 6 or 7 digits.
 *       • A letter typed after 6 digits signals "segment 2 is done" → space is inserted.
 *       • A 7th digit is still accepted (7-digit IDs exist).
 *       • A letter typed after 7 digits also signals "segment 2 is done" → space is inserted.
 *       • Any extra digits beyond 7 are ignored while we wait for the letter.
 *  3. Segment 3 — 1 uppercase letter followed by exactly 2 digits.
 *
 * The formatter works by stripping all non-alphanumeric characters from the raw
 * input and rebuilding the canonical form, so hyphens and spaces are always
 * inserted automatically — the user never needs to type them.
 */
private fun formatNationalId(raw: String): String {
    // Strip everything except digits and letters; force uppercase
    val clean = raw.filter { it.isDigit() || it.isLetter() }.uppercase()
    if (clean.isEmpty()) return ""

    val result = StringBuilder()
    var i = 0

    // ── Segment 1: exactly 2 digits ──────────────────────────────────────────
    var seg1 = 0
    while (i < clean.length && seg1 < 2) {
        val ch = clean[i]
        if (ch.isDigit()) { result.append(ch); seg1++; i++ }
        else break // non-digit before seg1 is complete → stop entirely
    }
    // Need both digits before we can continue
    if (seg1 < 2 || i >= clean.length) return result.toString()
    result.append('-')

    // ── Segment 2: 6 or 7 digits ─────────────────────────────────────────────
    // Strategy: collect digits one at a time.
    //   • After each digit, peek at the next character:
    //       – If we have ≥6 digits and the next char is a letter → done, break.
    //       – If we have 7 digits → stop regardless (max reached), wait for letter.
    //   • Letters appearing before 6 digits are silently skipped.
    var seg2 = 0
    seg2Loop@ while (i < clean.length) {
        val ch = clean[i]
        when {
            ch.isDigit() && seg2 < 7 -> {
                result.append(ch); seg2++; i++
                // After collecting this digit, peek ahead: if the next char is a letter
                // and we already have ≥6 digits, segment 2 is complete — exit the loop
                // so that the letter can be processed by segment 3.
                val next = clean.getOrNull(i)
                if (next != null && next.isLetter() && seg2 >= 6) break@seg2Loop
            }
            ch.isDigit() && seg2 == 7 -> {
                // Already at maximum 7 digits — ignore any further digits and wait for a letter
                i++
            }
            ch.isLetter() && seg2 >= 6 -> {
                // Letter encountered directly (without a digit peek) after ≥6 digits — done
                break@seg2Loop
            }
            ch.isLetter() && seg2 < 6 -> {
                // Letter too early (fewer than 6 middle digits) — skip it silently
                i++
            }
            else -> i++
        }
    }

    // Only continue to segment 3 if: we have ≥6 seg2 digits AND the next char is a letter
    if (seg2 < 6 || i >= clean.length || !clean[i].isLetter()) return result.toString()
    result.append(' ')

    // ── Segment 3: 1 letter + up to 2 digits ─────────────────────────────────
    var seg3Letter = false
    var seg3Digits = 0
    while (i < clean.length) {
        val ch = clean[i]
        when {
            ch.isLetter() && !seg3Letter -> { result.append(ch); seg3Letter = true; i++ }
            ch.isDigit() && seg3Letter && seg3Digits < 2 -> { result.append(ch); seg3Digits++; i++ }
            else -> i++ // skip extra characters
        }
    }

    return result.toString()
}

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
    selectedSubtype: String = "",
    authState: AuthState,
    onLogin: (String, String, Boolean) -> Unit,
    onNavigateAfterLogin: () -> Unit,
    onRegister: (
        name: String, email: String, password: String,
        phoneNumber: String, profilePhotoUrl: String, photoBytes: ByteArray?,
        gender: String, nationalId: String,
        providerSubtype: String,
        agentLicenseNumber: String, yearsOfExperience: String,
        companyName: String, companyRegNumber: String,
        companyAddress: String, taxId: String
    ) -> Unit,
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

    // Gender dropdown
    val genderOptions = listOf("Male", "Female", "Transgender", "Prefer not to say")
    var regGender          by remember { mutableStateOf("") }
    var showGenderDropdown by remember { mutableStateOf(false) }

    // National ID with auto-formatting (Zimbabwe format: ##-###### X##)
    // TextFieldValue is used instead of String so we can explicitly control
    // cursor position after each auto-format operation (e.g. placing cursor
    // after the auto-inserted hyphen, not before).
    var regNationalId by remember { mutableStateOf(TextFieldValue("")) }

    // Agent-specific fields
    var regLicenseNumber  by remember { mutableStateOf("") }
    var regYearsExp       by remember { mutableStateOf("") }
    var licenseError      by remember { mutableStateOf("") }
    // Brokerage-specific fields
    var regCompanyName    by remember { mutableStateOf("") }
    var regCompanyReg     by remember { mutableStateOf("") }
    var regCompanyAddress by remember { mutableStateOf("") }
    var regTaxId          by remember { mutableStateOf("") }
    var companyNameError  by remember { mutableStateOf("") }
    var companyRegError   by remember { mutableStateOf("") }
    var companyAddrError  by remember { mutableStateOf("") }

    // Validation errors — declared BEFORE imagePicker so the lambda can reference them
    var nameError      by remember { mutableStateOf("") }
    var emailError     by remember { mutableStateOf("") }
    var phoneError     by remember { mutableStateOf("") }
    var photoError     by remember { mutableStateOf("") }
    var passwordError  by remember { mutableStateOf("") }
    var confirmError   by remember { mutableStateOf("") }
    var genderError    by remember { mutableStateOf("") }
    var nationalIdError by remember { mutableStateOf("") }

    // Real platform image picker
    val imagePicker = rememberImagePickerLauncher { picked: PickedImage? ->
        if (picked != null) {
            regPhotoUri   = picked.uri
            regPhotoBytes = picked.bytes
            photoError    = ""
        }
    }

    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()

    Box(modifier = Modifier.fillMaxSize().imePadding().background(MaterialTheme.colorScheme.background)) {
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
                text = when {
                    selectedRole == "tenant"       -> "🔑 Tenant"
                    selectedSubtype == "agent"     -> "🤝 Freelancer Agent"
                    selectedSubtype == "brokerage" -> "🏢 Brokerage"
                    else                           -> "🏠 Landlord"
                },
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
                        var signInLoading by remember { mutableStateOf(false) }
                        // Tracks whether the 4s progress bar has fully completed
                        var signInBarDone by remember { mutableStateOf(false) }

                        // When the bar finishes AND Firebase already succeeded → navigate.
                        // When Firebase succeeds first → wait for bar → then navigate.
                        // If Firebase returns an error at any point → reset the button immediately.
                        LaunchedEffect(authState, signInBarDone) {
                            when {
                                signInLoading && signInBarDone && authState is AuthState.Success -> {
                                    signInLoading = false
                                    onNavigateAfterLogin()
                                }
                                signInLoading && authState is AuthState.Error -> {
                                    // Firebase failed — stop the bar and let the error message show
                                    signInLoading = false
                                    signInBarDone = false
                                }
                            }
                        }

                        ProgressButton(
                            itemCount = 1,
                            isLoading = signInLoading,
                            onClick = {
                                var valid = true
                                if (loginEmail.isBlank()) { emailError = "Email is required"; valid = false }
                                if (loginPassword.isBlank()) { passwordError = "Password is required"; valid = false }
                                if (valid) {
                                    signInBarDone = false
                                    signInLoading = true
                                    // Fire Firebase sign-in instantly, in parallel with the bar
                                    onLogin(loginEmail.trim(), loginPassword, rememberMe)
                                }
                            },
                            buttonText = "Sign In →",
                            loadingText = "Signing in",
                            successText = "",
                            onComplete = { signInBarDone = true },
                            variant = ProgressVariant.LINEAR,
                            animationDurationMs = 4000,
                            modifier = Modifier.fillMaxWidth()
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
                            errorMessage = nameError,
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.Words,
                                keyboardType = KeyboardType.Text,
                                imeAction = ImeAction.Next
                            )
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

                        // ── 4. Gender ─────────────────────────────────────────
                        // Custom Dialog-based gender picker with radio buttons,
                        // emoji icons, animated selection, and centered layout.
                        val genderMeta = remember {
                            listOf(
                                Triple("Male",            "👨",  Color(0xFF1565C0)),
                                Triple("Female",          "👩",  Color(0xFFAD1457)),
                                Triple("Transgender",     "🏳️\u200D⚧️", Color(0xFF6A1B9A)),
                                Triple("Prefer not to say","🤝", Color(0xFF37474F))
                            )
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                "Gender",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (genderError.isNotEmpty()) MaterialTheme.colorScheme.error
                                        else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(start = 4.dp, bottom = 6.dp).fillMaxWidth()
                            )

                            // ── Trigger pill ──────────────────────────────────
                            val triggerInteraction = remember { MutableInteractionSource() }
                            val isTriggerPressed by triggerInteraction.collectIsPressedAsState()
                            val triggerScale by animateFloatAsState(
                                targetValue = if (isTriggerPressed) 0.97f else 1f,
                                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                                label = "gender_trigger_scale"
                            )
                            val arrowRotation by animateFloatAsState(
                                targetValue = if (showGenderDropdown) 180f else 0f,
                                animationSpec = tween(250, easing = EaseInOutSine),
                                label = "gender_arrow_rot"
                            )
                            val selectedMeta = genderMeta.firstOrNull { it.first == regGender }
                            val triggerBorderColor by animateColorAsState(
                                targetValue = when {
                                    genderError.isNotEmpty() -> MaterialTheme.colorScheme.error
                                    regGender.isNotEmpty()   -> (selectedMeta?.third ?: RentOutColors.Primary).copy(alpha = 0.7f)
                                    else                     -> MaterialTheme.colorScheme.outline
                                },
                                animationSpec = tween(300),
                                label = "gender_border"
                            )

                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .scale(triggerScale)
                                    .clip(RoundedCornerShape(12.dp))
                                    .border(
                                        width = if (regGender.isNotEmpty() || genderError.isNotEmpty()) 1.5.dp else 1.dp,
                                        color = triggerBorderColor,
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .clickable(
                                        interactionSource = triggerInteraction,
                                        indication = null
                                    ) { showGenderDropdown = true },
                                color = MaterialTheme.colorScheme.surface,
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    // Leading: emoji if selected, else generic icon
                                    AnimatedContent(
                                        targetState = selectedMeta,
                                        transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(150)) },
                                        label = "gender_lead_icon"
                                    ) { meta ->
                                        if (meta != null) {
                                            Text(meta.second, fontSize = 20.sp)
                                        } else {
                                            Icon(
                                                Icons.Default.Person,
                                                contentDescription = null,
                                                tint = RentOutColors.IconTeal,
                                                modifier = Modifier.size(22.dp)
                                            )
                                        }
                                    }
                                    // Label / selected value
                                    AnimatedContent(
                                        targetState = regGender,
                                        transitionSpec = {
                                            slideInVertically { -it / 2 } + fadeIn() togetherWith
                                            slideOutVertically { it / 2 } + fadeOut()
                                        },
                                        label = "gender_label_anim",
                                        modifier = Modifier.weight(1f)
                                    ) { current ->
                                        Text(
                                            text = if (current.isEmpty()) "Select gender" else current,
                                            fontSize = 15.sp,
                                            fontWeight = if (current.isEmpty()) FontWeight.Normal else FontWeight.SemiBold,
                                            color = if (current.isEmpty())
                                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                            else
                                                selectedMeta?.third ?: MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                    // Animated dot indicator when selected
                                    AnimatedVisibility(
                                        visible = regGender.isNotEmpty(),
                                        enter = scaleIn(spring(Spring.DampingRatioMediumBouncy)) + fadeIn(),
                                        exit  = scaleOut() + fadeOut()
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .clip(CircleShape)
                                                .background(selectedMeta?.third ?: RentOutColors.Primary)
                                        )
                                    }
                                    Icon(
                                        Icons.Default.ArrowDropDown,
                                        contentDescription = null,
                                        modifier = Modifier
                                            .size(24.dp)
                                            .graphicsLayer { rotationZ = arrowRotation },
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            AnimatedVisibility(visible = genderError.isNotEmpty()) {
                                Text(
                                    genderError,
                                    color = MaterialTheme.colorScheme.error,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(start = 4.dp, top = 4.dp).fillMaxWidth()
                                )
                            }
                        }

                        // ── Gender picker Dialog ───────────────────────────────
                        if (showGenderDropdown) {
                            Dialog(
                                onDismissRequest = { showGenderDropdown = false },
                                properties = DialogProperties(usePlatformDefaultWidth = false)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 32.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Card(
                                        shape = RoundedCornerShape(24.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.surface
                                        ),
                                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column {
                                            // ── Gradient header ───────────────
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .background(
                                                        Brush.horizontalGradient(
                                                            listOf(RentOutColors.Primary, RentOutColors.Secondary)
                                                        ),
                                                        RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                                                    )
                                                    .padding(horizontal = 20.dp, vertical = 16.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                    Text(
                                                        "Select Gender",
                                                        fontSize = 18.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color.White
                                                    )
                                                    Text(
                                                        "Choose the option that best describes you",
                                                        fontSize = 11.sp,
                                                        color = Color.White.copy(alpha = 0.75f),
                                                        textAlign = TextAlign.Center
                                                    )
                                                }
                                            }

                                            // ── Options ───────────────────────
                                            Column(
                                                modifier = Modifier.padding(
                                                    start = 16.dp, end = 16.dp,
                                                    top = 12.dp, bottom = 16.dp
                                                ),
                                                verticalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                genderMeta.forEach { (option, emoji, accentColor) ->
                                                    val isSelected = regGender == option
                                                    val optInteraction = remember { MutableInteractionSource() }
                                                    val isOptPressed by optInteraction.collectIsPressedAsState()
                                                    val optScale by animateFloatAsState(
                                                        targetValue = if (isOptPressed) 0.96f else 1f,
                                                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                                                        label = "gender_opt_scale_$option"
                                                    )
                                                    val cardBg by animateColorAsState(
                                                        targetValue = if (isSelected)
                                                            accentColor.copy(alpha = 0.10f)
                                                        else
                                                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                                        animationSpec = tween(250),
                                                        label = "gender_card_bg_$option"
                                                    )
                                                    val cardBorder by animateColorAsState(
                                                        targetValue = if (isSelected)
                                                            accentColor.copy(alpha = 0.6f)
                                                        else
                                                            Color.Transparent,
                                                        animationSpec = tween(250),
                                                        label = "gender_card_border_$option"
                                                    )
                                                    // Animated radio fill
                                                    val radioFill by animateFloatAsState(
                                                        targetValue = if (isSelected) 1f else 0f,
                                                        animationSpec = spring(
                                                            dampingRatio = Spring.DampingRatioMediumBouncy,
                                                            stiffness = Spring.StiffnessMedium
                                                        ),
                                                        label = "radio_fill_$option"
                                                    )

                                                    Surface(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .scale(optScale)
                                                            .clip(RoundedCornerShape(14.dp))
                                                            .border(1.dp, cardBorder, RoundedCornerShape(14.dp))
                                                            .clickable(
                                                                interactionSource = optInteraction,
                                                                indication = null
                                                            ) {
                                                                regGender = option
                                                                genderError = ""
                                                                showGenderDropdown = false
                                                            },
                                                        color = cardBg,
                                                        shape = RoundedCornerShape(14.dp)
                                                    ) {
                                                        Row(
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .padding(horizontal = 14.dp, vertical = 12.dp),
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                                        ) {
                                                            // Emoji in a tinted circle
                                                            Box(
                                                                modifier = Modifier
                                                                    .size(40.dp)
                                                                    .clip(CircleShape)
                                                                    .background(accentColor.copy(alpha = 0.12f)),
                                                                contentAlignment = Alignment.Center
                                                            ) {
                                                                Text(emoji, fontSize = 20.sp)
                                                            }
                                                            // Label
                                                            Column(modifier = Modifier.weight(1f)) {
                                                                Text(
                                                                    option,
                                                                    fontSize = 15.sp,
                                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                                    color = if (isSelected) accentColor
                                                                            else MaterialTheme.colorScheme.onSurface
                                                                )
                                                            }
                                                            // Animated custom radio button
                                                            Box(
                                                                modifier = Modifier
                                                                    .size(22.dp)
                                                                    .clip(CircleShape)
                                                                    .border(
                                                                        width = 2.dp,
                                                                        color = if (isSelected) accentColor
                                                                                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                                                        shape = CircleShape
                                                                    ),
                                                                contentAlignment = Alignment.Center
                                                            ) {
                                                                // Inner filled dot that scales in/out
                                                                Box(
                                                                    modifier = Modifier
                                                                        .size((12 * radioFill).dp)
                                                                        .clip(CircleShape)
                                                                        .background(accentColor)
                                                                )
                                                            }
                                                        }
                                                    }
                                                }
                                            }

                                            // ── Dismiss button ────────────────
                                            TextButton(
                                                onClick = { showGenderDropdown = false },
                                                modifier = Modifier
                                                    .align(Alignment.CenterHorizontally)
                                                    .padding(bottom = 8.dp)
                                            ) {
                                                Text(
                                                    "Cancel",
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    fontSize = 13.sp
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        Spacer(Modifier.height(14.dp))

                        // ── 5. National ID ────────────────────────────────────
                        // Uses OutlinedTextField + TextFieldValue directly so we can
                        // pin the cursor to the end of the formatted string after each
                        // auto-format operation (hyphen/space insertion).
                        Column {
                            OutlinedTextField(
                                value = regNationalId,
                                onValueChange = { incoming ->
                                    val formatted = formatNationalId(incoming.text)
                                    // Always place cursor at end of the formatted result
                                    regNationalId = TextFieldValue(
                                        text = formatted,
                                        selection = TextRange(formatted.length)
                                    )
                                    nationalIdError = ""
                                },
                                label = { Text("National ID  (##-###### X##)", fontSize = 13.sp) },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Badge,
                                        contentDescription = null,
                                        tint = RentOutColors.IconBlue
                                    )
                                },
                                isError = nationalIdError.isNotEmpty(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            )
                            if (nationalIdError.isNotEmpty()) {
                                Text(
                                    text = nationalIdError,
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.padding(start = 12.dp, top = 4.dp)
                                )
                            }
                        }
                        Spacer(Modifier.height(14.dp))

                        // ── 6. Profile Photo ──────────────────────────────────
                        // ── 5b. Agent fields ─────────────────────────────────
                        AnimatedVisibility(
                            visible = selectedRole == "landlord" && selectedSubtype == "agent",
                            enter   = expandVertically() + fadeIn(),
                            exit    = shrinkVertically() + fadeOut()
                        ) {
                            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Divider(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.outlineVariant)
                                    Spacer(Modifier.width(10.dp))
                                    Surface(shape = RoundedCornerShape(20.dp), color = RentOutColors.Primary.copy(alpha = 0.10f)) {
                                        Text("🤝 Agent Details", fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
                                            color = RentOutColors.Primary, modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp))
                                    }
                                    Spacer(Modifier.width(10.dp))
                                    Divider(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.outlineVariant)
                                }
                                RentOutTextField(
                                    value = regLicenseNumber, onValueChange = { regLicenseNumber = it; licenseError = "" },
                                    label = "Agent License / Accreditation Number",
                                    leadingIcon = Icons.Default.Badge, leadingIconTint = RentOutColors.IconTeal,
                                    isError = licenseError.isNotEmpty(), errorMessage = licenseError
                                )
                                RentOutTextField(
                                    value = regYearsExp, onValueChange = { regYearsExp = it },
                                    label = "Years of Experience (optional)",
                                    leadingIcon = Icons.Default.WorkHistory, leadingIconTint = RentOutColors.IconAmber,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                                )
                            }
                        }
                        // ── 5c. Brokerage fields ──────────────────────────────
                        AnimatedVisibility(
                            visible = selectedRole == "landlord" && selectedSubtype == "brokerage",
                            enter   = expandVertically() + fadeIn(),
                            exit    = shrinkVertically() + fadeOut()
                        ) {
                            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Divider(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.outlineVariant)
                                    Spacer(Modifier.width(10.dp))
                                    Surface(shape = RoundedCornerShape(20.dp), color = RentOutColors.Primary.copy(alpha = 0.10f)) {
                                        Text("🏢 Company Details", fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
                                            color = RentOutColors.Primary, modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp))
                                    }
                                    Spacer(Modifier.width(10.dp))
                                    Divider(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.outlineVariant)
                                }
                                RentOutTextField(
                                    value = regCompanyName, onValueChange = { regCompanyName = it; companyNameError = "" },
                                    label = "Company / Agency Name",
                                    leadingIcon = Icons.Default.Business, leadingIconTint = RentOutColors.IconBlue,
                                    isError = companyNameError.isNotEmpty(), errorMessage = companyNameError
                                )
                                RentOutTextField(
                                    value = regCompanyReg, onValueChange = { regCompanyReg = it; companyRegError = "" },
                                    label = "Company Registration Number",
                                    leadingIcon = Icons.Default.Numbers, leadingIconTint = RentOutColors.IconTeal,
                                    isError = companyRegError.isNotEmpty(), errorMessage = companyRegError
                                )
                                RentOutTextField(
                                    value = regCompanyAddress, onValueChange = { regCompanyAddress = it; companyAddrError = "" },
                                    label = "Company / Office Address",
                                    leadingIcon = Icons.Default.LocationCity, leadingIconTint = RentOutColors.IconGreen,
                                    isError = companyAddrError.isNotEmpty(), errorMessage = companyAddrError
                                )
                                RentOutTextField(
                                    value = regTaxId, onValueChange = { regTaxId = it },
                                    label = "Tax ID / ZIMRA Number (optional)",
                                    leadingIcon = Icons.Default.Receipt, leadingIconTint = RentOutColors.IconAmber
                                )
                            }
                        }
                        Spacer(Modifier.height(14.dp))

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

                        // ── 7. Password ───────────────────────────────────────
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

                        // ── 8. Confirm Password ───────────────────────────────
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
                        var createAccountLoading by remember { mutableStateOf(false) }
                        var createAccountBarDone by remember { mutableStateOf(false) }

                        // Firebase registration runs in parallel with the 8s progress bar.
                        // Navigate (via AuthState.Registered) when BOTH the bar is done AND
                        // Firebase has succeeded. If Firebase errors at any point → reset immediately.
                        LaunchedEffect(authState, createAccountBarDone) {
                            when {
                                createAccountLoading && createAccountBarDone && authState is AuthState.Registered -> {
                                    createAccountLoading = false
                                }
                                createAccountLoading && authState is AuthState.Error -> {
                                    createAccountLoading = false
                                    createAccountBarDone = false
                                }
                            }
                        }

                        ProgressButton(
                            itemCount = 1,
                            isLoading = createAccountLoading,
                            onClick = {
                                // Reset all errors
                                nameError = ""; emailError = ""; phoneError = ""
                                photoError = ""; passwordError = ""; confirmError = ""
                                genderError = ""; nationalIdError = ""

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
                                if (regGender.isBlank())
                                    errors += FieldError({ genderError = it }, "Please select a gender", 280)
                                // National ID: validate format has all 3 segments complete
                                // Valid complete format examples: "63-123456 A78" or "63-1234567 A78"
                                val nationalIdValid = run {
                                    val parts = regNationalId.text.split("-")
                                    if (parts.size < 2) return@run false
                                    val seg1 = parts[0]
                                    val rest = parts[1] // e.g. "123456 A78"
                                    val seg2And3 = rest.split(" ")
                                    if (seg2And3.size < 2) return@run false
                                    val seg2 = seg2And3[0]
                                    val seg3 = seg2And3[1]
                                    seg1.length == 2 && seg1.all { it.isDigit() } &&
                                    seg2.length in 6..7 && seg2.all { it.isDigit() } &&
                                    seg3.length == 3 && seg3[0].isLetter() && seg3[1].isDigit() && seg3[2].isDigit()
                                }
                                if (regNationalId.text.isBlank())
                                    errors += FieldError({ nationalIdError = it }, "National ID is required", 350)
                                else if (!nationalIdValid)
                                    errors += FieldError({ nationalIdError = it }, "Enter a valid ID (e.g. 63-123456 A78)", 350)
                                if (regPhotoUri.isEmpty())
                                    errors += FieldError({ photoError = it }, "A profile photo is required", 480)
                                if (regPassword.length < 6)
                                    errors += FieldError({ passwordError = it }, "Password must be at least 6 characters", 620)
                                if (regPassword != regConfirm)
                                    errors += FieldError({ confirmError = it }, "Passwords do not match", 700)

                                if (errors.isNotEmpty()) {
                                    // Apply all errors so user sees every issue at once
                                    errors.forEach { it.setter(it.msg) }
                                    // Scroll to the first error
                                    coroutineScope.launch {
                                        scrollState.animateScrollTo(errors.first().scrollY)
                                    }
                                } else {
                                    createAccountBarDone = false
                                    createAccountLoading = true
                                    // Fire Firebase registration instantly, in parallel with the 8s bar
                                    val fullPhone = "${regCountry.code} ${regPhone.trim()}"
                                    // Subtype-specific validation
                                    var subtypeValid = true
                                    if (selectedSubtype == "agent" && regLicenseNumber.isBlank()) {
                                        licenseError = "License number is required"
                                        coroutineScope.launch { scrollState.animateScrollTo(600) }
                                        createAccountLoading = false
                                        subtypeValid = false
                                    }
                                    if (subtypeValid && selectedSubtype == "brokerage") {
                                        if (regCompanyName.isBlank()) { companyNameError = "Company name is required"; createAccountLoading = false; subtypeValid = false }
                                        else if (regCompanyReg.isBlank())  { companyRegError  = "Registration number is required"; createAccountLoading = false; subtypeValid = false }
                                        else if (regCompanyAddress.isBlank()) { companyAddrError = "Office address is required"; createAccountLoading = false; subtypeValid = false }
                                    }
                                    if (subtypeValid) {
                                        onRegister(
                                            regName.trim(), regEmail.trim(), regPassword,
                                            fullPhone, regPhotoUri, regPhotoBytes,
                                            regGender, regNationalId.text.trim(),
                                            selectedSubtype,
                                            regLicenseNumber.trim(), regYearsExp.trim(),
                                            regCompanyName.trim(), regCompanyReg.trim(),
                                            regCompanyAddress.trim(), regTaxId.trim()
                                        )
                                    }
                                }
                            },
                            buttonText = "Create Account 🎉",
                            loadingText = "Creating account",
                            successText = "Account created!",
                            onComplete = { createAccountBarDone = true },
                            variant = ProgressVariant.LINEAR,
                            animationDurationMs = 8000,
                            modifier = Modifier.fillMaxWidth()
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

    // Loading is handled entirely by the ProgressButton linear animation.

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

