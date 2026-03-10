@file:OptIn(ExperimentalMaterial3Api::class)

package org.example.project.ui.screens.landlord

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import org.example.project.data.model.Property
import org.example.project.presentation.PropertyFormState
import org.example.project.presentation.PropertyViewModel
import org.example.project.ui.components.RentOutPrimaryButton
import org.example.project.ui.theme.RentOutColors
import org.example.project.ui.util.ImagePickerLauncher
import org.example.project.ui.util.ImagePickerSource
import org.example.project.ui.util.PickedImage
import org.example.project.ui.util.rememberImagePickerLauncher
import org.example.project.ui.components.RemoveImageConfirmationDialog

// ── Residential mandatory photo requirements banner ───────────────────────────
@Composable
private fun ResidentialPhotoBanner() {
    val infiniteTransition = rememberInfiniteTransition(label = "banner_pulse")
    val borderAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1200, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "border_alpha"
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.5.dp,
                color = Color(0xFFF59E0B).copy(alpha = borderAlpha),
                shape = RoundedCornerShape(16.dp)
            ),
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFFFFFBEB)
    ) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(text = "⚠️", fontSize = 16.sp)
                Text(
                    text = "Mandatory Photos Required",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF92400E)
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Residential listings must include at least 5 photos covering:",
                fontSize = 12.sp,
                color = Color(0xFF78350F)
            )
            Spacer(Modifier.height(4.dp))
            val requirements = listOf(
                "🚿  The bathroom",
                "🚽  The toilet",
                "📐  At least 3 angles of each room"
            )
            requirements.forEach { req ->
                Row(
                    modifier = Modifier.padding(start = 8.dp, top = 3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = req, fontSize = 12.sp, color = Color(0xFF92400E))
                }
            }
        }
    }
}

@Composable
fun PropertyImagesScreen(
    property: Property,
    formState: PropertyFormState,
    viewModel: PropertyViewModel,
    onSubmit: (Property, List<ByteArray>) -> Unit,
    onBack: () -> Unit
) {
    // Restore images from the draft so they survive back-navigation
    val draft by viewModel.draft.collectAsState()
    var pickedImages by remember { mutableStateOf<List<PickedImage>>(draft.pickedImages) }
    var showSourceDialog by remember { mutableStateOf(false) }
    var showRemoveDialog by remember { mutableStateOf(false) }
    var imageToRemoveIndex by remember { mutableStateOf<Int?>(null) }
    val isLoading = formState is PropertyFormState.Uploading

    // Back button animation
    var backPressed by remember { mutableStateOf(false) }
    val backScale    by animateFloatAsState(if (backPressed) 0.8f else 1f, tween(200), label = "bs")
    val backRotation by animateFloatAsState(if (backPressed) -45f else 0f,  tween(200), label = "br")

    // Image picker
    val imagePicker: ImagePickerLauncher = rememberImagePickerLauncher { picked ->
        if (picked != null) {
            pickedImages = pickedImages + picked
        }
    }

    if (formState is PropertyFormState.Success) {
        LaunchedEffect(formState) { onBack() }
    }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {

        // Header gradient
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .background(
                    Brush.verticalGradient(
                        listOf(RentOutColors.Primary, RentOutColors.Primary.copy(alpha = 0f))
                    )
                )
        )

        Column(modifier = Modifier.fillMaxSize()) {

            // ── Top bar ──
            Spacer(Modifier.height(52.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
            ) {
                IconButton(
                    onClick = {
                        backPressed = true
                        // Persist current images into draft so they survive the round-trip
                        viewModel.saveDraft(draft.copy(pickedImages = pickedImages))
                        onBack()
                    },
                    modifier = Modifier.graphicsLayer {
                        scaleX = backScale; scaleY = backScale; rotationZ = backRotation
                    }
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White)
                }
                Spacer(Modifier.weight(1f))
                Text(
                    text = "Property Images",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.weight(1f))
                // Add image FAB — always visible
                AddImageFab(onClick = { showSourceDialog = true })
            }

            Spacer(Modifier.height(16.dp))

            // ── Property info chip ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color.White.copy(alpha = 0.2f),
                    modifier = Modifier.clip(RoundedCornerShape(20.dp))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Default.Home, null, tint = Color.White, modifier = Modifier.size(14.dp))
                        Text(
                            text = property.title.take(40),
                            fontSize = 13.sp,
                            color = Color.White,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // ── Main content card ──
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {

                    // ── Header row ──
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "📸 Add Photos",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${pickedImages.size} photo${if (pickedImages.size != 1) "s" else ""} selected",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        // (Add image FAB is always visible in the top bar)
                    }

                    Spacer(Modifier.height(16.dp))
                    Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                    Spacer(Modifier.height(16.dp))

                    // ── Residential mandatory photo banner ──
                    // Show only until the landlord has uploaded at least 2 images.
                    // After the 2nd image is added the banner animates away, freeing up space.
                    val isResidential = property.classification.equals("Residential", ignoreCase = true)
                    val showBanner = isResidential && pickedImages.size < 2
                    AnimatedVisibility(
                        visible = showBanner,
                        enter = fadeIn(tween(350)) + expandVertically(
                            animationSpec = tween(400, easing = FastOutSlowInEasing),
                            expandFrom = Alignment.Top
                        ),
                        exit = fadeOut(tween(300)) + shrinkVertically(
                            animationSpec = tween(380, easing = FastOutSlowInEasing),
                            shrinkTowards = Alignment.Top
                        )
                    ) {
                        Column {
                            ResidentialPhotoBanner()
                            Spacer(Modifier.height(16.dp))
                        }
                    }

                    // ── Image grid or empty state ──
                    if (pickedImages.isEmpty()) {
                        ImageEmptyState(onAddClick = { showSourceDialog = true })
                    } else {
                        // Fixed-height grid (not inside another scroll)
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            contentPadding = PaddingValues(bottom = 8.dp)
                        ) {
                            itemsIndexed(pickedImages) { index, image ->
                                ImagePreviewCard(
                                    image    = image,
                                    index    = index,
                                    onRemove = {
                                        imageToRemoveIndex = index
                                        showRemoveDialog = true
                                    }
                                )
                            }
                            // "Add more" tile
                            item {
                                AddMoreImageTile(onClick = { showSourceDialog = true })
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    // ── Submit for Review button ──
                    SubmitForReviewButton(
                        imageCount    = pickedImages.size,
                        isLoading     = isLoading,
                        isResidential = isResidential,
                        onClick       = {
                            onSubmit(property, pickedImages.map { it.bytes })
                        }
                    )

                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "⏳ Your listing goes to our admin team for review. Once approved, tenants can see it.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
                    )
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }

    // ── Image source dialog ──
    if (showSourceDialog) {
        ImageSourceDialog(
            onGallery = {
                showSourceDialog = false
                imagePicker.launch(ImagePickerSource.GALLERY)
            },
            onCamera = {
                showSourceDialog = false
                imagePicker.launch(ImagePickerSource.CAMERA)
            },
            onDismiss = { showSourceDialog = false }
        )
    }
    
    // ── Remove image confirmation dialog ──────────────────────────────────────
    if (showRemoveDialog && imageToRemoveIndex != null) {
        RemoveImageConfirmationDialog(
            imageType = "photo",
            onConfirm = {
                pickedImages = pickedImages.toMutableList().also { it.removeAt(imageToRemoveIndex!!) }
                showRemoveDialog = false
                imageToRemoveIndex = null
            },
            onDismiss = {
                showRemoveDialog = false
                imageToRemoveIndex = null
            }
        )
    }
}

// ── Add Image FAB ──
@Composable
private fun AddImageFab(onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.9f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "fab_scale"
    )
    Box(
        modifier = Modifier
            .scale(scale)
            .size(44.dp)
            .clip(CircleShape)
            .background(
                Brush.linearGradient(listOf(RentOutColors.Primary, RentOutColors.PrimaryLight))
            )
            .clickable(interactionSource = interactionSource, indication = null) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(Icons.Default.Add, contentDescription = "Add image", tint = Color.White, modifier = Modifier.size(22.dp))
    }
}

// ── Empty state ──
@Composable
private fun ImageEmptyState(onAddClick: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "empty_pulse")
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.95f, targetValue = 1.05f,
        animationSpec = infiniteRepeatable(tween(1000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "pulse"
    )
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier
                .scale(pulse)
                .size(100.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(RentOutColors.Primary.copy(alpha = 0.1f))
                .border(2.dp, RentOutColors.Primary.copy(alpha = 0.3f), RoundedCornerShape(24.dp))
                .clickable { onAddClick() },
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Default.AddPhotoAlternate,
                    contentDescription = null,
                    tint = RentOutColors.Primary,
                    modifier = Modifier.size(36.dp)
                )
                Spacer(Modifier.height(4.dp))
                Text("Add", fontSize = 12.sp, color = RentOutColors.Primary, fontWeight = FontWeight.Bold)
            }
        }
        Text(
            text = "No photos yet",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = "Great photos help attract tenants faster.\nAdd at least 3 photos.",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

// ── Image preview card ──
@Composable
private fun ImagePreviewCard(
    image: PickedImage,
    index: Int,
    onRemove: () -> Unit
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(index * 80L)
        visible = true
    }
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + scaleIn(initialScale = 0.8f)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
        ) {
            // Real image rendering via Coil from in-memory ByteArray
            coil3.compose.AsyncImage(
                model = image.bytes,
                contentDescription = "Photo ${index + 1}",
                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // Cover number badge
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(6.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color.Black.copy(alpha = 0.5f))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = if (index == 0) "Cover" else "#${index + 1}",
                    fontSize = 10.sp,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }

            // Remove button
            RemoveImageButton(
                modifier = Modifier.align(Alignment.TopEnd).padding(6.dp),
                onRemove = onRemove
            )
        }
    }
}

// ── Remove button ──
@Composable
private fun RemoveImageButton(modifier: Modifier = Modifier, onRemove: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.85f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "rm_scale"
    )
    Box(
        modifier = modifier
            .scale(scale)
            .size(28.dp)
            .clip(CircleShape)
            .background(Color.Red.copy(alpha = 0.85f))
            .clickable(interactionSource = interactionSource, indication = null) { onRemove() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            Icons.Default.Close,
            contentDescription = "Remove image",
            tint = Color.White,
            modifier = Modifier.size(16.dp)
        )
    }
}

// ── Add more tile ──
@Composable
private fun AddMoreImageTile(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(RoundedCornerShape(16.dp))
            .border(
                2.dp,
                RentOutColors.Primary.copy(alpha = 0.4f),
                RoundedCornerShape(16.dp)
            )
            .background(RentOutColors.Primary.copy(alpha = 0.05f))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                Icons.Default.AddPhotoAlternate,
                contentDescription = null,
                tint = RentOutColors.Primary.copy(alpha = 0.6f),
                modifier = Modifier.size(28.dp)
            )
            Text(
                text = "Add More",
                fontSize = 12.sp,
                color = RentOutColors.Primary.copy(alpha = 0.7f),
                fontWeight = FontWeight.Medium
            )
        }
    }
}

// ── Submit for Review button — morphs into an animated loading bar ──
@Composable
private fun SubmitForReviewButton(
    imageCount: Int,
    isLoading: Boolean,
    isResidential: Boolean,
    onClick: () -> Unit
) {
    // ── States ────────────────────────────────────────────────────────────────
    val minRequired = if (isResidential) 5 else 1
    val enabled = imageCount >= minRequired && !isLoading

    // Simulated upload progress: 0f → 1f over ~2.8 s while isLoading is true
    var simulatedProgress by remember { mutableStateOf(0f) }
    var isDone by remember { mutableStateOf(false) }

    LaunchedEffect(isLoading) {
        if (isLoading) {
            simulatedProgress = 0f
            isDone = false
            // Fill to 92% quickly, then hold — actual completion drives the rest
            val steps = 46          // 46 steps × 60 ms = ~2.76 s to reach 92 %
            repeat(steps) { i ->
                delay(60L)
                simulatedProgress = (i + 1) / 50f   // tops out at 0.92
            }
        } else if (simulatedProgress > 0f) {
            // Submission finished — snap to 100 % then show checkmark
            simulatedProgress = 1f
            delay(400)
            isDone = true
        }
    }

    // Animated progress value (smooth tween)
    val animatedProgress by animateFloatAsState(
        targetValue = simulatedProgress,
        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
        label = "upload_progress"
    )

    // Button press spring
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val buttonScale by animateFloatAsState(
        targetValue = if (isPressed) 0.94f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "submit_scale"
    )
    val elevation by animateDpAsState(
        targetValue = if (isPressed || isLoading) 2.dp else 10.dp,
        label = "submit_elev"
    )

    // Corner radius: 16 dp (button) → 28 dp (pill bar)
    val cornerRadius by animateDpAsState(
        targetValue = if (isLoading) 28.dp else 16.dp,
        animationSpec = tween(400, easing = FastOutSlowInEasing),
        label = "corner_anim"
    )

    // Shimmer sweep position (infinite while loading)
    val shimmerTransition = rememberInfiniteTransition(label = "shimmer")
    val shimmerOffset by shimmerTransition.animateFloat(
        initialValue = -1f, targetValue = 2f,
        animationSpec = infiniteRepeatable(tween(1200, easing = LinearEasing)),
        label = "shimmer_offset"
    )

    // Checkmark pop scale
    val checkScale by animateFloatAsState(
        targetValue = if (isDone) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness    = Spring.StiffnessMedium
        ),
        label = "check_scale"
    )

    // ── Layout ────────────────────────────────────────────────────────────────
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .scale(buttonScale)
            .shadow(if (enabled || isLoading) elevation else 0.dp, RoundedCornerShape(cornerRadius))
            .height(56.dp)
            .clip(RoundedCornerShape(cornerRadius))
            .then(
                if (enabled && !isLoading)
                    Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = onClick
                    )
                else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        if (isLoading || isDone) {
            // ── Loading bar track ─────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(RentOutColors.Primary.copy(alpha = 0.15f))
            )

            // Filled portion — gradient sweep
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(fraction = animatedProgress.coerceIn(0f, 1f))
                    .align(Alignment.CenterStart)
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                RentOutColors.Primary,
                                RentOutColors.PrimaryLight,
                                RentOutColors.Primary
                            )
                        )
                    )
            )

            // Shimmer highlight sweeping over the filled portion (hidden when done)
            if (!isDone) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(fraction = animatedProgress.coerceIn(0f, 1f))
                        .align(Alignment.CenterStart)
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.White.copy(alpha = 0.35f),
                                    Color.Transparent
                                ),
                                startX = shimmerOffset * 400f,
                                endX   = shimmerOffset * 400f + 300f
                            )
                        )
                )
            }

            // Content: percentage counter → checkmark
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                if (isDone) {
                    // Checkmark pop
                    Box(
                        modifier = Modifier
                            .scale(checkScale)
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.25f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = "Done",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Submitted!",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        letterSpacing = 0.3.sp
                    )
                } else {
                    // Spinning upload icon
                    val spinAngle by rememberInfiniteTransition(label = "spin")
                        .animateFloat(
                            initialValue = 0f, targetValue = 360f,
                            animationSpec = infiniteRepeatable(tween(900, easing = LinearEasing)),
                            label = "spin_angle"
                        )
                    Icon(
                        Icons.Default.CloudUpload,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier
                            .size(18.dp)
                            .graphicsLayer { rotationZ = spinAngle }
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = "Uploading… ${(animatedProgress * 100).toInt()}%",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        letterSpacing = 0.3.sp
                    )
                }
            }

        } else {
            // ── Normal button state ───────────────────────────────────────────
            val bgColor by animateColorAsState(
                targetValue = if (enabled) RentOutColors.Primary
                              else MaterialTheme.colorScheme.surfaceVariant,
                label = "submit_bg"
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(bgColor),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Default.Send,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = if (enabled) Color.White
                               else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = when {
                            imageCount == 0 -> "Add photos to submit"
                            isResidential && imageCount < minRequired -> "Add ${minRequired - imageCount} more photo${if (minRequired - imageCount != 1) "s" else ""} to submit"
                            else -> "Submit for Review"
                        },
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.3.sp,
                        color = if (enabled) Color.White
                                else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

// ── Image source picker dialog ──
@Composable
fun ImageSourceDialog(
    onGallery: () -> Unit,
    onCamera: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(20.dp),
        title = {
            Text(
                text = "Add Photo",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Choose where to pick your photo from:", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                // Gallery option
                ImageSourceOption(
                    icon  = Icons.Default.PhotoLibrary,
                    tint  = RentOutColors.IconBlue,
                    label = "Photo Gallery",
                    desc  = "Pick from your device",
                    onClick = onGallery
                )
                // Camera option
                ImageSourceOption(
                    icon  = Icons.Default.CameraAlt,
                    tint  = RentOutColors.IconTeal,
                    label = "Camera",
                    desc  = "Take a new photo",
                    onClick = onCamera
                )
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    )
}

@Composable
private fun ImageSourceOption(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    label: String,
    desc: String,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "src_opt_scale"
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .clickable(interactionSource = interactionSource, indication = null) { onClick() }
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(tint.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
        }
        Column {
            Text(label, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text(desc,  fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.weight(1f))
        Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
    }
}
