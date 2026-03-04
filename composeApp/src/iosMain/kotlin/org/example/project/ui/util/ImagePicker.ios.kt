package org.example.project.ui.util

import androidx.compose.runtime.Composable

/**
 * iOS stub — image picking on iOS requires UIImagePickerController or
 * PHPickerViewController, which need a UIKit bridge. This stub satisfies
 * the expect/actual contract so the project compiles. A full iOS
 * implementation can be added when the iOS target is being actively developed.
 */
@Composable
actual fun rememberImagePickerLauncher(
    onResult: (PickedImage?) -> Unit
): ImagePickerLauncher {
    return ImagePickerLauncher { _ ->
        // No-op on iOS until UIKit bridge is implemented
        onResult(null)
    }
}
