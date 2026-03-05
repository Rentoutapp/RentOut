package org.example.project.ui.util

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext

/**
 * Android actual implementation of [rememberImagePickerLauncher].
 *
 * Gallery: uses GetContent (ACTION_GET_CONTENT) — works on all API levels.
 * Camera:  uses TakePicture with a MediaStore URI — no FileProvider needed,
 *          works from API 24+ and respects scoped storage on API 29+.
 *
 * Camera permission is requested at runtime before launching the camera
 * intent, as required on Android 6.0+ (API 23+).
 */
@Composable
actual fun rememberImagePickerLauncher(
    onResult: (PickedImage?) -> Unit
): ImagePickerLauncher {
    val context = LocalContext.current

    // Holds the URI we created for the camera to write into
    var cameraUri by remember { mutableStateOf<Uri?>(null) }

    // ── Gallery launcher ──────────────────────────────────────────────────────
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri == null) { onResult(null); return@rememberLauncherForActivityResult }
        val bytes = context.contentResolver.openInputStream(uri)?.readBytes()
        if (bytes != null) onResult(PickedImage(uri.toString(), bytes))
        else onResult(null)
    }

    // ── Camera launcher ───────────────────────────────────────────────────────
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success: Boolean ->
        if (!success || cameraUri == null) { onResult(null); return@rememberLauncherForActivityResult }
        val bytes = context.contentResolver.openInputStream(cameraUri!!)?.readBytes()
        if (bytes != null) onResult(PickedImage(cameraUri.toString(), bytes))
        else onResult(null)
    }

    // ── Runtime camera permission request ────────────────────────────────────
    // Registered once; when granted it immediately launches the camera.
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            cameraUri = createCameraUri(context)
            cameraUri?.let { cameraLauncher.launch(it) }
        } else {
            Toast.makeText(
                context,
                "Camera permission is required to take photos.",
                Toast.LENGTH_LONG
            ).show()
            onResult(null)
        }
    }

    return remember {
        ImagePickerLauncher { source ->
            when (source) {
                ImagePickerSource.GALLERY -> galleryLauncher.launch("image/*")
                ImagePickerSource.CAMERA  -> {
                    // Request the CAMERA permission at runtime before launching.
                    // If already granted, the system calls back immediately with true.
                    cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                }
            }
        }
    }
}

/**
 * Creates a temporary MediaStore URI for the camera to write the captured
 * photo into. This approach avoids FileProvider setup entirely and works
 * correctly with Android scoped storage (API 29+).
 */
private fun createCameraUri(context: Context): Uri? {
    val contentValues = ContentValues().apply {
        put(MediaStore.Images.Media.DISPLAY_NAME, "profile_${System.currentTimeMillis()}.jpg")
        put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }
    }
    return context.contentResolver.insert(
        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
        contentValues
    )
}
