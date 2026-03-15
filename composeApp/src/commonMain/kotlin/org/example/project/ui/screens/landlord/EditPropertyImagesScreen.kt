package org.example.project.ui.screens.landlord

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import org.example.project.data.model.Property
import org.example.project.presentation.PropertyFormState
import org.example.project.presentation.PropertyViewModel
import org.example.project.ui.theme.RentOutColors
import org.example.project.ui.util.ImagePickerLauncher
import org.example.project.ui.util.ImagePickerSource
import org.example.project.ui.util.PickedImage
import org.example.project.ui.util.rememberImagePickerLauncher
import org.example.project.ui.components.RemoveImageConfirmationDialog

// ─────────────────────────────────────────────────────────────────────────────
// EditPropertyImagesScreen
// Shows ALL existing remote images + newly picked images.
// Each image has a red ✕ delete button.
// + Add Image FAB appears at top-right always.
// On save: passes kept remote URLs + new byte arrays to updateProperty().
// ─────────────────────────────────────────────────────────────────────────────

// ── Sealed type representing a previewable image (remote URL or local bytes) ──
private sealed class PreviewImage {
    data class Remote(val url: String) : PreviewImage()
    data class Local(val bytes: ByteArray) : PreviewImage()
}

@Composable
fun EditPropertyImagesScreen(
    property: Property,
    formState: PropertyFormState,
    viewModel: PropertyViewModel,
    onSave: (keepUrls: List<String>, newBytes: List<ByteArray>) -> Unit,
    onBack: () -> Unit
) {
    // Existing remote images — landlord can remove any of these
    // Use LaunchedEffect to update when property changes (e.g., after save)
    var keptRemoteUrls by remember(property.id) {
        val all = property.imageUrls.ifEmpty {
            listOfNotNull(property.imageUrl.takeIf { it.isNotBlank() })
        }
        println("📸 EditPropertyImagesScreen: Initializing keptRemoteUrls with ${all.size} images")
        all.forEachIndexed { idx, url -> println("   [$idx] $url") }
        mutableStateOf(all)
    }
    
    // Update keptRemoteUrls when property.imageUrls changes
    LaunchedEffect(property.imageUrls) {
        val all = property.imageUrls.ifEmpty {
            listOfNotNull(property.imageUrl.takeIf { it.isNotBlank() })
        }
        println("🔄 EditPropertyImagesScreen: property.imageUrls changed, updating keptRemoteUrls to ${all.size} images")
        all.forEachIndexed { idx, url -> println("   [$idx] $url") }
        keptRemoteUrls = all
    }

    // Newly picked local images
    var newImages by remember { mutableStateOf<List<PickedImage>>(emptyList()) }
    var showSourceDialog by remember { mutableStateOf(false) }
    var showRemoveDialog by remember { mutableStateOf(false) }
    var imageToRemove by remember { mutableStateOf<Pair<Boolean, Int>?>(null) } // (isRemote, index)

    // Preview state — index into combined [keptRemoteUrls + newImages] list
    var previewIndex by remember { mutableStateOf<Int?>(null) }

    // Full-screen preview dialog
    val allPreviewImages: List<PreviewImage> = remember(keptRemoteUrls, newImages) {
        keptRemoteUrls.map { PreviewImage.Remote(it) } +
        newImages.map { PreviewImage.Local(it.bytes) }
    }

    if (previewIndex != null) {
        androidx.compose.ui.window.Dialog(
            onDismissRequest = { previewIndex = null },
            properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
        ) {
            var currentIdx by remember { mutableStateOf(previewIndex!!) }
            val total = allPreviewImages.size

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.96f)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // ── Top bar ───────────────────────────────────────────────
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = if (currentIdx == 0) "Cover Photo" else "Photo ${currentIdx + 1} of $total",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                            val label = when (allPreviewImages.getOrNull(currentIdx)) {
                                is PreviewImage.Remote -> "Saved"
                                is PreviewImage.Local  -> "New — not saved yet"
                                else                   -> ""
                            }
                            Text(label, fontSize = 11.sp, color = Color.White.copy(0.6f))
                        }
                        IconButton(onClick = { previewIndex = null }) {
                            Icon(Icons.Default.Close, "Close", tint = Color.White, modifier = Modifier.size(24.dp))
                        }
                    }

                    // ── Main image with swipe gesture ─────────────────────────
                    var dragOffsetX by remember { mutableStateOf(0f) }
                    var swipeDirection by remember { mutableStateOf(1) } // 1=left, -1=right

                    val animatedOffsetX by animateFloatAsState(
                        targetValue = dragOffsetX,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMedium
                        ),
                        label = "swipe_offset"
                    )

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp)
                            .pointerInput(currentIdx, total) {
                                detectHorizontalDragGestures(
                                    onDragEnd = {
                                        when {
                                            dragOffsetX < -80f && currentIdx < total - 1 -> {
                                                swipeDirection = 1
                                                currentIdx++
                                            }
                                            dragOffsetX > 80f && currentIdx > 0 -> {
                                                swipeDirection = -1
                                                currentIdx--
                                            }
                                        }
                                        dragOffsetX = 0f
                                    },
                                    onDragCancel = { dragOffsetX = 0f },
                                    onHorizontalDrag = { _, dragAmount ->
                                        dragOffsetX = (dragOffsetX + dragAmount).coerceIn(-200f, 200f)
                                    }
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        AnimatedContent(
                            targetState = currentIdx,
                            transitionSpec = {
                                if (targetState > initialState) {
                                    // Swiping forward
                                    (slideInHorizontally { it } + fadeIn()) togetherWith
                                    (slideOutHorizontally { -it } + fadeOut())
                                } else {
                                    // Swiping backward
                                    (slideInHorizontally { -it } + fadeIn()) togetherWith
                                    (slideOutHorizontally { it } + fadeOut())
                                }
                            },
                            label = "image_transition"
                        ) { idx ->
                            when (val img = allPreviewImages.getOrNull(idx)) {
                                is PreviewImage.Remote -> AsyncImage(
                                    model = img.url,
                                    contentDescription = "Preview",
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .fillMaxHeight(0.78f)
                                        .clip(RoundedCornerShape(16.dp))
                                        .graphicsLayer {
                                            translationX = animatedOffsetX
                                            alpha = 1f - (kotlin.math.abs(animatedOffsetX) / 400f)
                                                .coerceIn(0f, 0.4f)
                                        }
                                )
                                is PreviewImage.Local -> AsyncImage(
                                    model = img.bytes,
                                    contentDescription = "Preview",
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .fillMaxHeight(0.78f)
                                        .clip(RoundedCornerShape(16.dp))
                                        .graphicsLayer {
                                            translationX = animatedOffsetX
                                            alpha = 1f - (kotlin.math.abs(animatedOffsetX) / 400f)
                                                .coerceIn(0f, 0.4f)
                                        }
                                )
                                else -> {}
                            }
                        }

                        // Swipe hint indicators
                        if (currentIdx > 0) {
                            Icon(
                                Icons.Default.ChevronLeft,
                                null,
                                tint = Color.White.copy(
                                    alpha = (kotlin.math.abs(dragOffsetX) / 200f).coerceIn(0f, 0.8f)
                                        .let { if (dragOffsetX > 0) it else 0f }
                                ),
                                modifier = Modifier
                                    .align(Alignment.CenterStart)
                                    .size(40.dp)
                                    .padding(start = 4.dp)
                            )
                        }
                        if (currentIdx < total - 1) {
                            Icon(
                                Icons.Default.ChevronRight,
                                null,
                                tint = Color.White.copy(
                                    alpha = (kotlin.math.abs(dragOffsetX) / 200f).coerceIn(0f, 0.8f)
                                        .let { if (dragOffsetX < 0) it else 0f }
                                ),
                                modifier = Modifier
                                    .align(Alignment.CenterEnd)
                                    .size(40.dp)
                                    .padding(end = 4.dp)
                            )
                        }
                    }

                    // ── Thumbnail strip + arrows ───────────────────────────────
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        IconButton(
                            onClick = { if (currentIdx > 0) currentIdx-- },
                            enabled = currentIdx > 0
                        ) {
                            Icon(
                                Icons.Default.ChevronLeft, "Previous",
                                tint = if (currentIdx > 0) Color.White else Color.White.copy(0.3f),
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        LazyRow(
                            modifier = Modifier.weight(1f),
                            horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally)
                        ) {
                            items(allPreviewImages) { img ->
                                val i = allPreviewImages.indexOf(img)
                                val isActive = i == currentIdx
                                val isRemote = img is PreviewImage.Remote
                                Box(
                                    modifier = Modifier
                                        .size(if (isActive) 54.dp else 44.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .border(
                                            2.dp,
                                            if (isActive) RentOutColors.Primary
                                            else if (isRemote) RentOutColors.IconBlue.copy(0.5f)
                                            else RentOutColors.StatusApproved.copy(0.5f),
                                            RoundedCornerShape(8.dp)
                                        )
                                        .clickable { currentIdx = i }
                                ) {
                                    when (img) {
                                        is PreviewImage.Remote -> AsyncImage(
                                            model = img.url,
                                            contentDescription = null,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                        is PreviewImage.Local -> AsyncImage(
                                            model = img.bytes,
                                            contentDescription = null,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                }
                            }
                        }
                        IconButton(
                            onClick = { if (currentIdx < total - 1) currentIdx++ },
                            enabled = currentIdx < total - 1
                        ) {
                            Icon(
                                Icons.Default.ChevronRight, "Next",
                                tint = if (currentIdx < total - 1) Color.White else Color.White.copy(0.3f),
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }
    // ── Derive upload progress from formState ─────────────────────────────────
    // Uploading(uploaded, total) drives the 0→100% bar.
    // When total == 0 (only kept URLs, no new uploads) the bar jumps straight
    // to 100% as soon as Uploading is emitted, giving instant visual feedback
    // for the Firestore metadata-only write.
    val isLoading = formState is PropertyFormState.Uploading || formState is PropertyFormState.Success
    val uploadedCount = (formState as? PropertyFormState.Uploading)?.uploaded ?: 0
    val uploadTotal   = (formState as? PropertyFormState.Uploading)?.total   ?: 0

    // ── Navigate back once the progress bar animation completes ───────────────
    // We let the ProgressButton's onComplete callback drive navigation so the
    // user always sees the bar reach 100% + checkmark before the screen pops.
    // resetFormState() is called inside onComplete so the next visit starts clean.
    var navigateBack by remember { mutableStateOf(false) }
    LaunchedEffect(navigateBack) {
        if (navigateBack) {
            viewModel.resetFormState()
            onBack()
        }
    }

    // Back button animation
    var backPressed by remember { mutableStateOf(false) }
    val backScale    by animateFloatAsState(if (backPressed) 0.8f else 1f, tween(200), label = "bs")
    val backRotation by animateFloatAsState(if (backPressed) -45f else 0f,  tween(200), label = "br")

    val totalCount = keptRemoteUrls.size + newImages.size

    // Image picker
    val imagePicker: ImagePickerLauncher = rememberImagePickerLauncher { picked ->
        if (picked != null) newImages = newImages + picked
    }

    // ── Animated navy→teal gradient — consistent with landlord dashboard ──────
    val bgTransition = rememberInfiniteTransition(label = "edit_img_bg")
    val bgShift by bgTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(8_000, easing = LinearEasing), RepeatMode.Reverse),
        label = "bg_shift"
    )
    val bgNavy     = Color(0xFF0E1C3E)
    val bgNavyMid  = Color(0xFF1A2B5E)
    val bgTealDark = Color(0xFF007A75)
    val bgTeal     = Color(0xFF00B4AE)
    val bgTop      = lerp(bgNavy,     bgNavyMid,  bgShift * 0.4f)
    val bgBottom   = lerp(bgTealDark, bgTeal,     bgShift * 0.6f)
    val bodyGradient = Brush.verticalGradient(listOf(bgTop, bgBottom))

    Box(modifier = Modifier.fillMaxSize().background(bodyGradient)) {

        Column(modifier = Modifier.fillMaxSize()) {

            Spacer(Modifier.height(52.dp))

            // ── Top bar ───────────────────────────────────────────────────────
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)
            ) {
                IconButton(
                    onClick = { backPressed = true; onBack() },
                    modifier = Modifier.graphicsLayer {
                        scaleX = backScale; scaleY = backScale; rotationZ = backRotation
                    }
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White)
                }
                Spacer(Modifier.weight(1f))
                Text(
                    "Edit Photos",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.weight(1f))
                // Add image FAB — always visible
                EditAddImageFab(onClick = { showSourceDialog = true })
            }

            Spacer(Modifier.height(16.dp))

            // ── Property title chip ───────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color.White.copy(alpha = 0.2f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Default.Home, null, tint = Color.White, modifier = Modifier.size(14.dp))
                        Text(
                            property.title.take(40),
                            fontSize = 13.sp,
                            color = Color.White,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // ── Main card ─────────────────────────────────────────────────────
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(8.dp)
            ) {
                Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {

                    // ── Header row ────────────────────────────────────────────
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                "📸 Manage Photos",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                "$totalCount photo${if (totalCount != 1) "s" else ""} total",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(Modifier.height(10.dp))

                    // ── Legend chips ──────────────────────────────────────────
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        LegendChip(
                            color = RentOutColors.IconBlue,
                            label = "Existing (${keptRemoteUrls.size})"
                        )
                        LegendChip(
                            color = RentOutColors.StatusApproved,
                            label = "New (${newImages.size})"
                        )
                    }

                    Spacer(Modifier.height(12.dp))
                    Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                    Spacer(Modifier.height(12.dp))

                    // ── Residential mandatory photo banner ────────────────────
                    // Show only while the landlord has fewer than 2 photos total
                    // (existing + new). Once 2 images are present the banner has
                    // served its purpose and slides away to give the grid more space.
                    val isResidential = property.classification.equals("Residential", ignoreCase = true)
                    val showEditBanner = isResidential && totalCount < 2
                    AnimatedVisibility(
                        visible = showEditBanner,
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
                            ResidentialPhotoBannerEdit()
                            Spacer(Modifier.height(12.dp))
                        }
                    }

                    // ── Image grid ────────────────────────────────────────────
                    if (totalCount == 0) {
                        // Empty state
                        EditImagesEmptyState(onAddClick = { showSourceDialog = true })
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            modifier = Modifier.fillMaxWidth().weight(1f),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            contentPadding = PaddingValues(bottom = 8.dp)
                        ) {
                            // ── Existing remote images ─────────────────────
                            itemsIndexed(keptRemoteUrls) { index, url ->
                                RemoteImageCard(
                                    url      = url,
                                    index    = index,
                                    isCover  = index == 0,
                                    onPreview = { previewIndex = index },
                                    onRemove = {
                                        imageToRemove = Pair(true, index)
                                        showRemoveDialog = true
                                    }
                                )
                            }

                            // ── Newly picked images ────────────────────────
                            itemsIndexed(newImages) { index, image ->
                                NewImageCard(
                                    image    = image,
                                    index    = keptRemoteUrls.size + index,
                                    onPreview = { previewIndex = keptRemoteUrls.size + index },
                                    onRemove = {
                                        imageToRemove = Pair(false, index)
                                        showRemoveDialog = true
                                    }
                                )
                            }

                            // ── Add more tile ──────────────────────────────
                            item {
                                EditAddMoreTile(onClick = { showSourceDialog = true })
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    // ── Save button — linear 0 → 100% progress bar ───────────
                    // The animation duration is gauged to the number of images:
                    //   • No new uploads (metadata-only): 1 200 ms fast sweep
                    //   • 1 new image: ~9 000 ms (real upload ~9 s from log)
                    //   • Each additional image adds ~4 000 ms
                    // The bar is driven by real Uploading(uploaded,total) state
                    // so it advances in locked step with actual Firebase Storage
                    // progress. When the upload finishes before the animation
                    // completes the bar snaps cleanly to 100%.
                    val minRequired = if (isResidential) 5 else 1
                    val saveEnabled = totalCount >= minRequired
                    val newCount    = newImages.size
                    val gaugedDurationMs = when {
                        newCount == 0 -> 1_200
                        newCount == 1 -> 9_000
                        else          -> 9_000 + (newCount - 1) * 4_000
                    }
                    // Real progress fraction: 0f when idle/no uploads,
                    // advances per completed image, reaches 1f on Success.
                    val realProgress: Float = when {
                        formState is PropertyFormState.Success -> 1f
                        uploadTotal == 0 -> if (isLoading) 1f else 0f
                        else -> uploadedCount.toFloat() / uploadTotal.toFloat()
                    }
                    EditPhotoProgressButton(
                        enabled          = saveEnabled,
                        isLoading        = isLoading,
                        totalCount       = totalCount,
                        minRequired      = minRequired,
                        realProgress     = realProgress,
                        animDurationMs   = gaugedDurationMs,
                        onComplete       = { navigateBack = true },
                        onClick          = {
                            println("💾 EditPropertyImagesScreen: Save button clicked")
                            println("   keptRemoteUrls (${keptRemoteUrls.size}): $keptRemoteUrls")
                            println("   newImages count: ${newImages.size}")
                            onSave(keptRemoteUrls, newImages.map { it.bytes })
                        }
                    )

                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Tap ✕ on any photo to remove it. Changes save immediately.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }

    // ── Source picker dialog ──────────────────────────────────────────────────
    if (showSourceDialog) {
        ImageSourceDialog(
            onGallery = { showSourceDialog = false; imagePicker.launch(ImagePickerSource.GALLERY) },
            onCamera  = { showSourceDialog = false; imagePicker.launch(ImagePickerSource.CAMERA) },
            onDismiss = { showSourceDialog = false }
        )
    }
    
    // ── Remove image confirmation dialog ──────────────────────────────────────
    if (showRemoveDialog && imageToRemove != null) {
        RemoveImageConfirmationDialog(
            imageType = if (imageToRemove!!.first) "existing photo" else "new photo",
            onConfirm = {
                val (isRemote, index) = imageToRemove!!
                if (isRemote) {
                    keptRemoteUrls = keptRemoteUrls.toMutableList().also { it.removeAt(index) }
                } else {
                    newImages = newImages.toMutableList().also { it.removeAt(index) }
                }
                showRemoveDialog = false
                imageToRemove = null
            },
            onDismiss = {
                showRemoveDialog = false
                imageToRemove = null
            }
        )
    }
}

// ── Existing remote image card ────────────────────────────────────────────────
@Composable
private fun RemoteImageCard(
    url: String,
    index: Int,
    isCover: Boolean,
    onPreview: () -> Unit = {},
    onRemove: () -> Unit
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(index * 60L)
        visible = true
    }
    AnimatedVisibility(visible = visible, enter = fadeIn() + scaleIn(initialScale = 0.85f)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .border(
                    2.dp,
                    RentOutColors.IconBlue.copy(alpha = 0.5f),
                    RoundedCornerShape(16.dp)
                )
                .clickable { onPreview() }
        ) {
            AsyncImage(
                model = url,
                contentDescription = "Photo ${index + 1}",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            // Cover / index badge
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(6.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (isCover) RentOutColors.Primary else Color.Black.copy(alpha = 0.5f))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    if (isCover) "Cover" else "#${index + 1}",
                    fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold
                )
            }
            // "Saved" indicator
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(6.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(RentOutColors.IconBlue.copy(alpha = 0.85f))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text("Saved", fontSize = 9.sp, color = Color.White, fontWeight = FontWeight.SemiBold)
            }
            // Delete button
            ImageDeleteButton(
                modifier = Modifier.align(Alignment.TopEnd).padding(6.dp),
                onRemove = onRemove
            )
        }
    }
}

// ── Newly picked image card ───────────────────────────────────────────────────
@Composable
private fun NewImageCard(
    image: PickedImage,
    index: Int,
    onPreview: () -> Unit = {},
    onRemove: () -> Unit
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(index * 60L)
        visible = true
    }
    AnimatedVisibility(visible = visible, enter = fadeIn() + scaleIn(initialScale = 0.85f)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .border(
                    2.dp,
                    RentOutColors.StatusApproved.copy(alpha = 0.6f),
                    RoundedCornerShape(16.dp)
                )
                .clickable { onPreview() }
        ) {
            coil3.compose.AsyncImage(
                model = image.bytes,
                contentDescription = "New photo",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            // "New" badge
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(6.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(RentOutColors.StatusApproved.copy(alpha = 0.9f))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text("New", fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold)
            }
            // Delete button
            ImageDeleteButton(
                modifier = Modifier.align(Alignment.TopEnd).padding(6.dp),
                onRemove = onRemove
            )
        }
    }
}

// ── Shared delete button ──────────────────────────────────────────────────────
@Composable
private fun ImageDeleteButton(modifier: Modifier = Modifier, onRemove: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        if (isPressed) 0.82f else 1f,
        spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "del_scale"
    )
    Box(
        modifier = modifier
            .scale(scale)
            .size(30.dp)
            .clip(CircleShape)
            .background(Color.Red.copy(alpha = 0.88f))
            .clickable(interactionSource = interactionSource, indication = null) { onRemove() },
        contentAlignment = Alignment.Center
    ) {
        Icon(Icons.Default.Close, "Remove", tint = Color.White, modifier = Modifier.size(16.dp))
    }
}

// ── Add Image FAB (top-right in header) ──────────────────────────────────────
@Composable
private fun EditAddImageFab(onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        if (isPressed) 0.88f else 1f,
        spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "add_fab_scale"
    )
    Box(
        modifier = Modifier
            .scale(scale)
            .size(44.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.22f))
            .clickable(interactionSource = interactionSource, indication = null) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(Icons.Default.Add, "Add image", tint = Color.White, modifier = Modifier.size(22.dp))
    }
}

// ── Add more tile in grid ─────────────────────────────────────────────────────
@Composable
private fun EditAddMoreTile(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(RoundedCornerShape(16.dp))
            .border(2.dp, RentOutColors.Primary.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
            .background(RentOutColors.Primary.copy(alpha = 0.05f))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                Icons.Default.AddPhotoAlternate, null,
                tint = RentOutColors.Primary.copy(alpha = 0.7f),
                modifier = Modifier.size(28.dp)
            )
            Text(
                "Add Photo",
                fontSize = 12.sp,
                color = RentOutColors.Primary.copy(alpha = 0.8f),
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

// ── Empty state ───────────────────────────────────────────────────────────────
@Composable
private fun EditImagesEmptyState(onAddClick: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulse by infiniteTransition.animateFloat(
        0.95f, 1.05f,
        infiniteRepeatable(tween(1000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "pulse_val"
    )
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .scale(pulse)
                .size(96.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(RentOutColors.Primary.copy(alpha = 0.1f))
                .border(2.dp, RentOutColors.Primary.copy(alpha = 0.3f), RoundedCornerShape(24.dp))
                .clickable { onAddClick() },
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.AddPhotoAlternate, null, tint = RentOutColors.Primary, modifier = Modifier.size(36.dp))
                Spacer(Modifier.height(4.dp))
                Text("Add", fontSize = 12.sp, color = RentOutColors.Primary, fontWeight = FontWeight.Bold)
            }
        }
        Text("No photos yet", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        Text(
            "All existing photos were removed.\nAdd new ones or go back to keep previous photos.",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

// ── Legend chip ───────────────────────────────────────────────────────────────
@Composable
private fun LegendChip(color: Color, label: String) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(color.copy(alpha = 0.1f))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(color))
        Text(label, fontSize = 11.sp, color = color, fontWeight = FontWeight.SemiBold)
    }
}

// ── Residential mandatory photo requirements banner (Edit screen) ─────────────
@Composable
private fun ResidentialPhotoBannerEdit() {
    val infiniteTransition = rememberInfiniteTransition(label = "edit_banner_pulse")
    val borderAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1200, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "edit_border_alpha"
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

// ── Save Photos progress button ───────────────────────────────────────────────
// A bespoke linear-fill button that:
//   1. Shows "Save Photos" when idle and enabled.
//   2. On click: starts a tween from 0 → realProgress (per-image real progress),
//      and simultaneously animates a time-gauged sweep toward 100%.
//   3. When realProgress reaches 1f (all uploads + Firestore done) the bar snaps
//      to 100%, shows a ✓ checkmark pop, then fires onComplete → navigate back.
//   4. The gauged sweep acts as a visual floor so the bar always appears to be
//      moving even when a single large image takes several seconds.
@Composable
private fun EditPhotoProgressButton(
    enabled: Boolean,
    isLoading: Boolean,
    totalCount: Int,
    minRequired: Int,
    realProgress: Float,       // 0f → 1f driven by Uploading(uploaded,total)
    animDurationMs: Int,       // gauged total animation window in ms
    onComplete: () -> Unit,
    onClick: () -> Unit
) {
    // ── States ────────────────────────────────────────────────────────────────
    var isDone        by remember { mutableStateOf(false) }
    var hasFiredBack  by remember { mutableStateOf(false) }

    // ── Gauged sweep: animates from 0 → 0.92 over animDurationMs as the visual
    //    floor. The real progress can jump ahead of this whenever uploads finish.
    var gaugedTarget  by remember { mutableStateOf(0f) }
    LaunchedEffect(isLoading) {
        if (isLoading) {
            isDone       = false
            hasFiredBack = false
            gaugedTarget = 0f
            // Animate the floor sweep in small steps so it feels organic
            val steps      = 60
            val stepDelay  = animDurationMs.toLong() / steps
            val stepSize   = 0.92f / steps
            repeat(steps) {
                kotlinx.coroutines.delay(stepDelay)
                gaugedTarget = (gaugedTarget + stepSize).coerceAtMost(0.92f)
            }
        } else {
            gaugedTarget = 0f
        }
    }

    // ── Snap to 100% and show checkmark when the real upload is done ──────────
    LaunchedEffect(realProgress) {
        if (realProgress >= 1f && isLoading && !isDone) {
            gaugedTarget = 1f
            kotlinx.coroutines.delay(350) // let bar visually reach 100%
            isDone = true
            kotlinx.coroutines.delay(600) // show checkmark briefly
            if (!hasFiredBack) {
                hasFiredBack = true
                onComplete()
            }
        }
    }

    // ── Animated values ───────────────────────────────────────────────────────
    // displayProgress = max(gaugedSweep, realProgress) so the bar never goes
    // backward and always reflects whichever is further ahead.
    val displayProgress = maxOf(gaugedTarget, realProgress).coerceIn(0f, 1f)

    val animatedProgress by animateFloatAsState(
        targetValue    = displayProgress,
        animationSpec  = tween(durationMillis = 300, easing = LinearEasing),
        label          = "edit_save_progress"
    )

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed         by interactionSource.collectIsPressedAsState()
    val buttonScale       by animateFloatAsState(
        targetValue   = if (isPressed) 0.95f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label         = "edit_save_scale"
    )
    val cornerRadius by animateDpAsState(
        targetValue   = if (isLoading) 28.dp else 16.dp,
        animationSpec = tween(400, easing = FastOutSlowInEasing),
        label         = "edit_save_corner"
    )
    val elevation by animateDpAsState(
        targetValue = if (isLoading || isPressed) 2.dp else if (enabled) 8.dp else 0.dp,
        label       = "edit_save_elev"
    )

    // Shimmer sweep while uploading
    val shimmerTransition = rememberInfiniteTransition(label = "edit_shimmer")
    val shimmerOffset by shimmerTransition.animateFloat(
        initialValue   = -1f,
        targetValue    = 2f,
        animationSpec  = infiniteRepeatable(tween(1200, easing = LinearEasing), RepeatMode.Restart),
        label          = "edit_shimmer_offset"
    )

    // Checkmark pop scale
    val checkScale by animateFloatAsState(
        targetValue   = if (isDone) 1f else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label         = "edit_check_scale"
    )

    // Upload arrow bounce
    val arrowTransition = rememberInfiniteTransition(label = "edit_arrow")
    val arrowOffset by arrowTransition.animateFloat(
        initialValue  = -4f,
        targetValue   = 4f,
        animationSpec = infiniteRepeatable(tween(500, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label         = "edit_arrow_offset"
    )

    // ── Layout ────────────────────────────────────────────────────────────────
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .scale(buttonScale)
            .shadow(elevation, RoundedCornerShape(cornerRadius))
            .height(56.dp)
            .clip(RoundedCornerShape(cornerRadius))
            .then(
                if (enabled && !isLoading)
                    Modifier.clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
                else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        if (isLoading || isDone) {
            // ── Progress / Done state ─────────────────────────────────────────
            // Track background
            Box(Modifier.fillMaxSize().background(RentOutColors.Primary.copy(alpha = 0.15f)))

            // Filled progress bar with gradient
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(animatedProgress.coerceIn(0f, 1f))
                    .align(Alignment.CenterStart)
                    .background(
                        Brush.horizontalGradient(
                            listOf(RentOutColors.Primary, RentOutColors.PrimaryLight, RentOutColors.Primary)
                        )
                    )
            )

            // Shimmer overlay (hidden when done)
            if (!isDone) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(animatedProgress.coerceIn(0f, 1f))
                        .align(Alignment.CenterStart)
                        .background(
                            Brush.horizontalGradient(
                                colors  = listOf(Color.Transparent, Color.White.copy(alpha = 0.30f), Color.Transparent),
                                startX  = shimmerOffset * 400f,
                                endX    = shimmerOffset * 400f + 300f
                            )
                        )
                )
            }

            // Content: percentage + upload arrow → checkmark
            Row(
                verticalAlignment    = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier             = Modifier.fillMaxSize()
            ) {
                if (isDone) {
                    Box(
                        modifier = Modifier
                            .scale(checkScale)
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.25f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Check, "Done", tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                    Spacer(Modifier.width(8.dp))
                    Text("Photos Saved!", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                } else {
                    Text(
                        "↑",
                        fontSize     = 18.sp,
                        fontWeight   = FontWeight.Bold,
                        color        = Color.White,
                        modifier     = Modifier.graphicsLayer { translationY = arrowOffset * density }
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Saving… ${(animatedProgress * 100).toInt()}%",
                        fontSize     = 14.sp,
                        fontWeight   = FontWeight.Bold,
                        color        = Color.White,
                        letterSpacing = 0.3.sp
                    )
                }
            }

        } else {
            // ── Idle state ────────────────────────────────────────────────────
            val bgColor by animateColorAsState(
                targetValue = if (enabled) RentOutColors.Primary else MaterialTheme.colorScheme.surfaceVariant,
                label       = "edit_save_bg"
            )
            Box(Modifier.fillMaxSize().background(bgColor), contentAlignment = Alignment.Center) {
                Row(
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.Save, null,
                        tint     = if (enabled) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = when {
                            totalCount == 0                                    -> "Add at least one photo"
                            !enabled                                           -> "Add ${minRequired - totalCount} more photo${if (minRequired - totalCount != 1) "s" else ""} to save"
                            else                                               -> "Save Photos"
                        },
                        color      = if (enabled) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold,
                        fontSize   = 15.sp
                    )
                }
            }
        }
    }
}
