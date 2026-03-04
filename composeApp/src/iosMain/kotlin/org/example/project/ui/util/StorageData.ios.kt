package org.example.project.ui.util

import dev.gitlive.firebase.storage.Data
import platform.Foundation.NSData
import platform.Foundation.create
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned

/**
 * On iOS, [Data] is a typealias for [NSData]. Convert [ByteArray] → [NSData].
 */
@OptIn(ExperimentalForeignApi::class)
actual fun buildStorageData(bytes: ByteArray): Data {
    return bytes.usePinned { pinned ->
        NSData.create(bytes = pinned.addressOf(0), length = bytes.size.toULong())
    }
}
