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
    val isLoading = formState is PropertyFormState.Uploading

    // Back button animation
    var backPressed by remember { mutableStateOf(false) }
    val backScale    by animateFloatAsState(if (backPressed) 0.8f else 1f, tween(200), label = "bs")
    val backRotation by animateFloatAsState(if (backPressed) -45f else 0f,  tween(200), label = "br")

    val totalCount = keptRemoteUrls.size + newImages.size

    // Image picker
    val imagePicker: ImagePickerLauncher = rememberImagePickerLauncher { picked ->
        if (picked != null) newImages = newImages + picked
    }

    // Navigate back on success
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
                    val isResidential = property.classification.equals("Residential", ignoreCase = true)
                    if (isResidential) {
                        ResidentialPhotoBannerEdit()
                        Spacer(Modifier.height(12.dp))
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

                    // ── Save button ───────────────────────────────────────────
                    EditSaveButton(
                        isLoading     = isLoading,
                        totalCount    = totalCount,
                        isResidential = isResidential,
                        onClick       = {
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

// ── Save button ───────────────────────────────────────────────────────────────
@Composable
private fun EditSaveButton(
    isLoading: Boolean,
    totalCount: Int,
    isResidential: Boolean,
    onClick: () -> Unit
) {
    val minRequired = if (isResidential) 5 else 1
    val enabled = totalCount >= minRequired && !isLoading
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        if (isPressed) 0.95f else 1f,
        spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "save_scale"
    )
    val bgColor by animateColorAsState(
        if (enabled) RentOutColors.Primary else MaterialTheme.colorScheme.surfaceVariant,
        label = "save_bg"
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .shadow(if (enabled) 8.dp else 0.dp, RoundedCornerShape(16.dp))
            .height(56.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(bgColor)
            .then(
                if (enabled) Modifier.clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
                else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        if (isLoading) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                CircularProgressIndicator(
                    color = Color.White,
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp
                )
                Text("Saving photos…", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.Save, null,
                    tint = if (enabled) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    when {
                        totalCount == 0 -> "Add at least one photo"
                        isResidential && totalCount < minRequired -> "Add ${minRequired - totalCount} more photo${if (minRequired - totalCount != 1) "s" else ""} to save"
                        else -> "Save Photos"
                    },
                    color = if (enabled) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }
        }
    }
}
