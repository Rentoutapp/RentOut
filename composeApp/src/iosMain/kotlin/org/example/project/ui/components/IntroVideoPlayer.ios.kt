package org.example.project.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.interop.UIKitView
import kotlinx.cinterop.ExperimentalForeignApi
import platform.AVFoundation.*
import platform.AVKit.AVPlayerViewController
import platform.CoreMedia.CMTimeMake
import platform.Foundation.NSBundle
import platform.Foundation.NSURL
import platform.UIKit.UIView

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun IntroVideoPlayer(
    modifier: Modifier,
    onVideoEnded: () -> Unit
) {
    // Resolve intro_vid.mp4 from the iOS app bundle
    val videoUrl: NSURL? = remember {
        NSBundle.mainBundle.URLForResource("intro_vid", withExtension = "mp4")
    }

    if (videoUrl == null) {
        // Video not bundled — skip straight to onVideoEnded so the app still works
        LaunchedEffect(Unit) { onVideoEnded() }
        Box(modifier = modifier.fillMaxSize().background(Color.Black))
        return
    }

    val player = remember {
        AVPlayer(uRL = videoUrl).also { p ->
            // Observe end of playback
            AVPlayerItemDidPlayToEndTimeNotification
        }
    }

    val playerViewController = remember {
        AVPlayerViewController().also { vc ->
            vc.player = player
            vc.showsPlaybackControls = false
            vc.videoGravity = AVLayerVideoGravityResizeAspectFill
        }
    }

    // Listen for video end via KVO / notification
    DisposableEffect(player) {
        val observer = platform.Foundation.NSNotificationCenter.defaultCenter.addObserverForName(
            name      = AVPlayerItemDidPlayToEndTimeNotification,
            `object`  = player.currentItem,
            queue     = platform.Foundation.NSOperationQueue.mainQueue,
            usingBlock = { _ -> onVideoEnded() }
        )
        player.play()
        onDispose {
            platform.Foundation.NSNotificationCenter.defaultCenter.removeObserver(observer)
            player.pause()
        }
    }

    UIKitView(
        factory = {
            val containerView = UIView()
            playerViewController.view.setFrame(containerView.bounds)
            containerView.addSubview(playerViewController.view)
            containerView
        },
        modifier = modifier
    )
}
