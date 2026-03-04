package org.example.project.ui.util

import androidx.compose.runtime.Composable

/**
 * Holds the result of a user image pick/capture action.
 * [uri] is a platform-specific string (Android content URI or file URI).
 * [bytes] is the raw image bytes ready to upload.
 */
data class PickedImage(val uri: String, val bytes: ByteArray)

/**
 * Platform-specific image picker launcher.
 * Call [launch] with the desired [ImagePickerSource] to start the picker.
 * The [onResult] callback is invoked on the main thread with the picked image,
 * or null if the user cancelled.
 */
class ImagePickerLauncher(
    val launch: (ImagePickerSource) -> Unit
)

enum class ImagePickerSource { CAMERA, GALLERY }

/**
 * Remembers and returns an [ImagePickerLauncher] for the current platform.
 * Must be called from a @Composable context.
 */
@Composable
expect fun rememberImagePickerLauncher(
    onResult: (PickedImage?) -> Unit
): ImagePickerLauncher
