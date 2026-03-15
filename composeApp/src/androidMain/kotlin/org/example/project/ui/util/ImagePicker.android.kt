package org.example.project.ui.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import java.io.File

/**
 * Android actual implementation of [rememberImagePickerLauncher].
 *
 * Gallery: uses GetContent (ACTION_GET_CONTENT) — works on all API levels.
 * Camera:  uses TakePicture with a FileProvider content:// URI pointing to a
 *          temp file in the app's private cache directory. This is the
 *          Android-recommended approach (developer.android.com/training/camera)
 *          and avoids the MediaStore IS_PENDING pattern that crashes many OEM
 *          camera apps (e.g., Samsung, Xiaomi) on Android 10+.
 *
 * Why FileProvider instead of MediaStore:
 * - MediaStore.EXTERNAL_CONTENT_URI with IS_PENDING=1 requires the camera app
 *   to hold WRITE_EXTERNAL_STORAGE, which is not guaranteed on modern Android.
 *   Many OEM camera apps (Samsung, Xiaomi, OPPO etc.) crash with
 *   "Camera keeps stopping" when given a MediaStore pending URI they cannot
 *   write to under their own sandbox.
 * - FileProvider grants a per-URI FLAG_GRANT_WRITE_URI_PERMISSION to the camera
 *   app for our private cache file, which is always accessible regardless of
 *   external storage permissions. This approach works on all Android 7.0+ (API 24+)
 *   devices reliably across all OEMs.
 *
 * BUG FIX: cameraUriString is stored in rememberSaveable so it survives
 * activity recreation when the camera app takes over the foreground (Android
 * may kill the host activity under memory pressure). Using plain remember{}
 * would lose the URI on recreation, causing the TakePicture callback to see
 * null and silently discard the captured photo.
 */
@Composable
actual fun rememberImagePickerLauncher(
    onResult: (PickedImage?) -> Unit
): ImagePickerLauncher {
    val context = LocalContext.current

    // ── Survives activity recreation (camera app kills host under memory pressure) ──
    // The FileProvider URI string is saved in the Bundle so we can read the
    // temp file back in the TakePicture callback even if the activity was killed.
    var cameraUriString by rememberSaveable { mutableStateOf<String?>(null) }

    // ── Gallery launcher ──────────────────────────────────────────────────────
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri == null) { onResult(null); return@rememberLauncherForActivityResult }
        val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
        if (bytes != null && bytes.isNotEmpty()) onResult(PickedImage(uri.toString(), bytes))
        else onResult(null)
    }

    // ── Camera launcher ───────────────────────────────────────────────────────
    // TakePicture writes the full-resolution JPEG to the URI we supply.
    // On success=true, we read the bytes from our private cache file.
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success: Boolean ->
        val savedUri = cameraUriString?.let { Uri.parse(it) }
        // Always clean up the temp file afterwards, regardless of success.
        val tempFile = cameraUriString?.let { uriStr ->
            // Reconstruct the File path from the URI string for cleanup.
            // We delete it after reading to avoid filling the cache.
            try {
                val cacheDir = File(context.cacheDir, "camera_captures")
                cacheDir.listFiles()?.firstOrNull { f ->
                    FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        f
                    ).toString() == uriStr
                }
            } catch (_: Exception) { null }
        }

        if (!success || savedUri == null) {
            tempFile?.delete()
            cameraUriString = null
            onResult(null)
            return@rememberLauncherForActivityResult
        }

        try {
            val bytes = context.contentResolver.openInputStream(savedUri)?.use { it.readBytes() }
            if (bytes != null && bytes.isNotEmpty()) {
                onResult(PickedImage(savedUri.toString(), bytes))
            } else {
                onResult(null)
            }
        } catch (e: Exception) {
            onResult(null)
        } finally {
            // Delete the temp cache file — bytes are already in memory.
            tempFile?.delete()
            cameraUriString = null
        }
    }

    // ── Launch camera after permission is confirmed ───────────────────────────
    // Extracted as a local function so both the permission callback and the
    // already-granted path can reuse the same logic without duplication.
    fun launchCamera() {
        val uri = createCameraFileUri(context)
        if (uri != null) {
            cameraUriString = uri.toString()
            cameraLauncher.launch(uri)
        } else {
            Toast.makeText(
                context,
                "Could not create image file. Please try again.",
                Toast.LENGTH_LONG
            ).show()
            onResult(null)
        }
    }

    // ── Runtime camera permission request ────────────────────────────────────
    // Registered once per composition. When the permission is granted, it
    // immediately launches the camera without requiring another user tap.
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            launchCamera()
        } else {
            Toast.makeText(
                context,
                "Camera permission is required to take photos.",
                Toast.LENGTH_LONG
            ).show()
            onResult(null)
        }
    }

    // Do NOT wrap in remember {} — the lambda must always close over the
    // current launcher references so it picks up any recomposition changes.
    return ImagePickerLauncher { source ->
        when (source) {
            ImagePickerSource.GALLERY -> galleryLauncher.launch("image/*")
            ImagePickerSource.CAMERA  -> {
                // Check if permission is already granted before requesting.
                // On Android 6.0+ (API 23+) CAMERA is a dangerous permission
                // that must be granted at runtime. If already granted, the
                // permission launcher calls back immediately with true — but
                // checking first avoids showing the system dialog unnecessarily.
                val alreadyGranted = ContextCompat.checkSelfPermission(
                    context, Manifest.permission.CAMERA
                ) == PackageManager.PERMISSION_GRANTED

                if (alreadyGranted) {
                    launchCamera()
                } else {
                    cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                }
            }
        }
    }
}

/**
 * Creates a temporary JPEG file in the app's private cache directory and
 * returns a FileProvider content:// URI that grants the camera app write
 * access to it.
 *
 * The authority "${packageName}.fileprovider" must match the FileProvider
 * declaration in AndroidManifest.xml and the paths in file_provider_paths.xml.
 *
 * The camera app receives FLAG_GRANT_WRITE_URI_PERMISSION for this URI
 * automatically via TakePicture (ActivityResultContracts). No external
 * storage permissions are required on either side.
 */
private fun createCameraFileUri(context: Context): Uri? {
    return try {
        val cacheDir = File(context.cacheDir, "camera_captures").apply { mkdirs() }
        // Use a timestamp-based name to avoid collisions when multiple photos
        // are taken in the same session before the previous temp file is deleted.
        val tempFile = File(cacheDir, "capture_${System.currentTimeMillis()}.jpg")
        FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            tempFile
        )
    } catch (e: Exception) {
        null
    }
}
