package com.kargathra.fitness.ui.components

import android.net.Uri
import android.widget.VideoView
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.kargathra.fitness.R

/**
 * Full-screen branded splash: plays res/raw/splash_intro once, then calls
 * [onDone]. Tap anywhere to skip. Any playback error skips immediately —
 * the splash must never block the app from opening.
 */
@Composable
fun SplashVideo(onDone: () -> Unit) {
    val context = LocalContext.current
    val uri = remember {
        Uri.parse("android.resource://${context.packageName}/${R.raw.splash_intro}")
    }
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onDone() },
        contentAlignment = Alignment.Center
    ) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                VideoView(ctx).apply {
                    setVideoURI(uri)
                    setOnCompletionListener { onDone() }
                    setOnErrorListener { _, _, _ -> onDone(); true }
                    setOnPreparedListener { it.isLooping = false; start() }
                }
            },
            onRelease = { it.stopPlayback() }
        )
    }
}
