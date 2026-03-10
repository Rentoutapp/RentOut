package org.example.project.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Platform-specific full-screen video player for the intro screen.
 * Android: ExoPlayer playing res/raw/intro_vid.mp4
 * iOS:     AVPlayer playing intro_vid.mp4 bundled in the Xcode project
 *
 * [onVideoEnded] is called when the video finishes playing once.
 */
@Composable
expect fun IntroVideoPlayer(
    modifier: Modifier = Modifier,
    onVideoEnded: () -> Unit
)
