package org.example.project.ui.util

import dev.gitlive.firebase.storage.Data

/**
 * On Android, [Data] wraps a [ByteArray].
 * The actual class constructor accepts a ByteArray directly.
 */
actual fun buildStorageData(bytes: ByteArray): Data = Data(bytes)
