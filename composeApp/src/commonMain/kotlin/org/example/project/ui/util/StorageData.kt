package org.example.project.ui.util

import dev.gitlive.firebase.storage.Data

/**
 * Converts a [ByteArray] into the platform-specific [Data] type required
 * by [dev.gitlive.firebase.storage.StorageReference.putData].
 */
expect fun buildStorageData(bytes: ByteArray): Data
