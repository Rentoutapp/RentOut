package org.example.project.ui.components

import android.net.Uri
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@Composable
actual fun IntroVideoPlayer(
    modifier: Modifier,
    onVideoEnded: () -> Unit
) {
    val context = LocalContext.current

    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().also { player ->
            val uri = Uri.parse(
                "android.resource://${context.packageName}/raw/intro_vid"
            )
            player.setMediaItem(MediaItem.fromUri(uri))
            player.repeatMode  = Player.REPEAT_MODE_OFF
            player.playWhenReady = true
            player.prepare()
        }
    }

    // Listen for playback end
    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_ENDED) {
                    onVideoEnded()
                }
            }
        }
        exoPlayer.addListener(listener)
        onDispose {
            exoPlayer.removeListener(listener)
            exoPlayer.release()
        }
    }

    AndroidView(
        factory = { ctx ->
            PlayerView(ctx).apply {
                player        = exoPlayer
                useController = false   // hide all playback controls
                // Scale video to fill screen (crop if needed)
                resizeMode    = androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_ZOOM
            }
        },
        modifier = modifier
    )
}
